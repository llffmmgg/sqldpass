package com.sqldpass.controller.content;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.content.dto.ContentSnapshotResponse;
import com.sqldpass.service.content.ContentSnapshotService;

import lombok.RequiredArgsConstructor;

/**
 * 네이티브 안드로이드 앱 첫 부트(또는 ETag 변경 시) 호출되는 콘텐츠 prefetch 엔드포인트.
 * 비로그인 호출 가능 — 보안 경계는 풀이 권한 체크(/api/mock-exams/...)와 결제 검증에 있다.
 */
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentSnapshotController {

    private final ContentSnapshotService contentSnapshotService;

    @GetMapping("/snapshot")
    public ResponseEntity<ContentSnapshotResponse> snapshot(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        String version = contentSnapshotService.currentVersion();
        String etag = "\"" + version + "\"";
        if (ifNoneMatch != null && etag.equals(ifNoneMatch.trim())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        ContentSnapshotResponse body = contentSnapshotService.buildSnapshot();
        return ResponseEntity.ok().eTag(etag).body(body);
    }
}
