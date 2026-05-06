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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
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
        return new PresignedUpload(signed.url().toString(), publicUrlFor(key), key, props.getMaxBytes());
    }

    private static String stripTrailingSlash(String s) {
        return s != null && s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    /**
     * R2 public URL 은 path 에 bucket name 을 포함해야 정상 응답한다.
     * 환경변수가 ".../<bucket>" 으로 끝나면 그대로 사용,
     * bucket 이 빠진 root 만 들어 있으면 자동으로 "/<bucket>" 을 붙여서
     * 일관된 형태(.../bucket/key) 의 URL 을 만든다.
     */
    private String normalizedPublicBaseUrl() {
        String base = stripTrailingSlash(props.getPublicBaseUrl());
        String bucket = props.getBucket();
        if (base != null && bucket != null && !bucket.isBlank() && !base.endsWith("/" + bucket)) {
            return base + "/" + bucket;
        }
        return base;
    }

    /**
     * 임의 바이트 객체를 R2 에 직접 업로드. (이미지 외 — 모의고사 PDF 캐시용)
     * 이미지 업로드와 달리 백엔드가 직접 PUT 한다 (presigned 거치지 않음).
     */
    public String uploadBytes(String key, byte[] body, String contentType) {
        return uploadBytes(key, body, contentType, null);
    }

    /**
     * contentDisposition 까지 같이 PUT — R2 가 객체 응답 시 그 헤더를 그대로 내려준다.
     * 누가 R2 public URL 을 직접 알아내도 브라우저가 미리보기 대신 다운로드로 처리.
     */
    public String uploadBytes(String key, byte[] body, String contentType, String contentDisposition) {
        if (!isEnabled()) {
            throw new SqldpassException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "R2 업로드가 설정되지 않았습니다.");
        }
        PutObjectRequest.Builder b = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(contentType);
        if (contentDisposition != null && !contentDisposition.isBlank()) {
            b.contentDisposition(contentDisposition);
        }
        s3Client.putObject(b.build(), RequestBody.fromBytes(body));
        return publicUrlFor(key);
    }

    /** R2 에서 객체 바이트를 받아온다. PDF 다운로드 프록시 용도. */
    public byte[] downloadBytes(String key) {
        if (!isEnabled()) {
            throw new SqldpassException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "R2 가 설정되지 않았습니다.");
        }
        return s3Client.getObjectAsBytes(software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build()).asByteArray();
    }

    /**
     * 객체 존재 여부 확인. PDF 캐시 hit 판정용.
     * 존재하면 publicUrl, 없으면 null.
     */
    public String publicUrlIfExists(String key) {
        if (!isEnabled()) return null;
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .build());
            return publicUrlFor(key);
        } catch (NoSuchKeyException e) {
            return null;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            // R2 는 missing 시 404 (statusCode 404) 를 반환 — NoSuchKeyException 이 안 잡힐 수도 있음.
            if (e.statusCode() == 404) return null;
            throw e;
        }
    }

    /** 키에 대응하는 public URL. */
    public String publicUrlFor(String key) {
        return normalizedPublicBaseUrl() + "/" + key;
    }

    public record PresignedUpload(
            String uploadUrl,   // 프론트가 PUT 호출할 URL (5분 유효)
            String publicUrl,   // 업로드 완료 후 마크다운에 삽입할 공개 URL
            String key,         // R2 오브젝트 키 (감사용)
            long maxBytes       // 프론트 검증용 한도
    ) {}
}
