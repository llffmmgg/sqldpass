package com.sqldpass.controller.blog;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.payment.SubscriptionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "블로그 다운로드", description = "블로그 글 첨부 PDF 다운로드 API")
@RestController
@RequestMapping("/api/blog-downloads")
@RequiredArgsConstructor
public class BlogDownloadController {

    private static final String ADSP_CRAM_RESOURCE = "blog-downloads/adsp-cram.pdf";
    private static final String ADSP_CRAM_FILENAME = "adsp_문어_벼락치기.pdf";

    private final SubscriptionService subscriptionService;

    @GetMapping("/adsp-cram")
    @Operation(
            summary = "ADsP D-1 정리본 PDF 다운로드 (Thunder 이상 활성 구독 회원 한정)",
            description = "정적 PDF 를 백엔드에서 권한 검증 후 바이트 스트리밍. "
                    + "모의고사 PDF(UNLIMITED 전용) 와 정책이 달라 활성 구독 여부만 체크 — "
                    + "Thunder/Focus/Pro/UNLIMITED 모두 가능. "
                    + "프론트 public 에 두지 않아 직접 URL 접근 불가."
    )
    public ResponseEntity<byte[]> downloadAdspCram(HttpServletRequest request) throws Exception {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        if (subscriptionService.getActive(memberId).isEmpty()) {
            throw new SqldpassException(ErrorCode.PDF_REQUIRES_SUBSCRIPTION);
        }

        ClassPathResource resource = new ClassPathResource(ADSP_CRAM_RESOURCE);
        byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());

        String encoded = URLEncoder.encode(ADSP_CRAM_FILENAME, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(bytes);
    }
}
