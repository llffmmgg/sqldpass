package com.sqldpass.service.upload;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sqldpass.config.R2Properties;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * Cloudflare R2 (S3 호환) 업로드 — presigned PUT URL 발급.
 *
 * 흐름:
 *   1. 프론트가 POST /api/uploads/image-url { contentType } 호출
 *   2. 백엔드가 presigned PUT URL + public URL 발급
 *   3. 프론트가 받은 URL 로 직접 PUT (multipart 거치지 않음 — 백엔드 부하 0)
 *   4. 업로드 완료 후 public URL 을 markdown 본문에 삽입
 *
 * 환경변수가 비어 있으면 enabled=false → 컨트롤러에서 503 반환.
 */
@Service
public class R2UploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif"
    );

    private final R2Properties props;
    private S3Client s3Client;
    private S3Presigner presigner;

    public R2UploadService(R2Properties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        if (!props.isEnabled()) return;
        AwsBasicCredentials creds = AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey());
        StaticCredentialsProvider provider = StaticCredentialsProvider.create(creds);
        URI endpoint = URI.create(props.getEndpoint());

        this.s3Client = S3Client.builder()
                .endpointOverride(endpoint)
                .region(Region.of("auto"))      // R2 는 region 'auto' 사용
                .credentialsProvider(provider)
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .region(Region.of("auto"))
                .credentialsProvider(provider)
                .build();
    }

    @PreDestroy
    void close() {
        if (s3Client != null) s3Client.close();
        if (presigner != null) presigner.close();
    }

    public boolean isEnabled() {
        return props.isEnabled();
    }

    public long maxBytes() {
        return props.getMaxBytes();
    }

    /**
     * presigned PUT URL 발급. 5분 유효.
     *
     * @param contentType 업로드할 파일의 MIME (이미지만 허용)
     * @param memberId    파일 키에 포함되는 owner 식별자 (감사 로그·정리용)
     */
    public PresignedUpload presign(String contentType, Long memberId) {
        if (!isEnabled()) {
            throw new SqldpassException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "이미지 업로드가 설정되지 않았습니다.");
        }
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "허용된 이미지 형식이 아닙니다. (png/jpg/webp/gif)");
        }

        String ext = switch (contentType.toLowerCase()) {
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            case "image/gif"  -> "gif";
            default           -> "jpg";
        };
        String key = "post/" + memberId + "/" + UUID.randomUUID() + "." + ext;

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(put)
                .build();

        PresignedPutObjectRequest signed = presigner.presignPutObject(presignReq);
        String publicUrl = stripTrailingSlash(props.getPublicBaseUrl()) + "/" + key;
        return new PresignedUpload(signed.url().toString(), publicUrl, key, props.getMaxBytes());
    }

    private static String stripTrailingSlash(String s) {
        return s != null && s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    public record PresignedUpload(
            String uploadUrl,   // 프론트가 PUT 호출할 URL (5분 유효)
            String publicUrl,   // 업로드 완료 후 마크다운에 삽입할 공개 URL
            String key,         // R2 오브젝트 키 (감사용)
            long maxBytes       // 프론트 검증용 한도
    ) {}
}
