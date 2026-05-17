package com.sqldpass.controller.content;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.content.dto.ContentSnapshotResponse;
import com.sqldpass.service.content.ContentSnapshotService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mobile/content")
@RequiredArgsConstructor
public class MobileContentSnapshotController {

    private final ContentSnapshotService contentSnapshotService;

    @GetMapping("/snapshot")
    public ResponseEntity<ContentSnapshotResponse> snapshot(
            HttpServletRequest request,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        Long memberId = (Long) request.getAttribute("memberId");
        String version = contentSnapshotService.currentMobileVersion(memberId);
        String etag = "\"" + version + "\"";
        if (ifNoneMatch != null && etag.equals(ifNoneMatch.trim())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .body(contentSnapshotService.buildMobileSnapshot(memberId));
    }
}
