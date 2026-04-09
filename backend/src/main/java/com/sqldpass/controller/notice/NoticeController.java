package com.sqldpass.controller.notice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.notice.dto.NoticeResponse;
import com.sqldpass.persistent.notice.NoticeDisplayType;
import com.sqldpass.service.notice.SiteNoticeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "공개 - 공지사항", description = "활성 공지 조회 (비로그인)")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final SiteNoticeService noticeService;

    @GetMapping("/active")
    @Operation(summary = "활성 공지 조회 (없으면 204)")
    public ResponseEntity<NoticeResponse> active(@RequestParam NoticeDisplayType type) {
        return noticeService.getActive(type)
                .map(NoticeResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
