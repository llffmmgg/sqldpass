package com.sqldpass.controller.upload;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.upload.R2UploadService;
import com.sqldpass.service.upload.R2UploadService.PresignedUpload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@Tag(name = "업로드", description = "Cloudflare R2 presigned URL 발급")
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final R2UploadService uploadService;

    public record PresignRequest(@NotBlank String contentType) {}

    @PostMapping("/image-url")
    @Operation(summary = "게시판 이미지 업로드용 presigned PUT URL 발급 (회원 전용)")
    public ResponseEntity<PresignedUpload> presignImage(
            HttpServletRequest request,
            @Valid @RequestBody PresignRequest body) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        if (!uploadService.isEnabled()) {
            // R2 환경변수 미설정 → 503
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(uploadService.presign(body.contentType(), memberId));
    }
}
