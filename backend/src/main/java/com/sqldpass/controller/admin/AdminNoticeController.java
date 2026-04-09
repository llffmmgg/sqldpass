package com.sqldpass.controller.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.admin.dto.CreateNoticeRequest;
import com.sqldpass.controller.admin.dto.UpdateNoticeActiveRequest;
import com.sqldpass.controller.admin.dto.UpdateNoticeRequest;
import com.sqldpass.controller.notice.dto.NoticeResponse;
import com.sqldpass.domain.notice.SiteNotice;
import com.sqldpass.service.notice.SiteNoticeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 - 공지사항", description = "사이트 공지(배너/모달) CRUD")
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final SiteNoticeService noticeService;

    @GetMapping
    @Operation(summary = "공지사항 목록")
    public List<NoticeResponse> list() {
        return noticeService.listAll().stream().map(NoticeResponse::from).toList();
    }

    @PostMapping
    @Operation(summary = "공지사항 생성")
    public NoticeResponse create(@Valid @RequestBody CreateNoticeRequest req) {
        SiteNotice created = noticeService.create(req.displayType(), req.title(), req.body(), req.active());
        return NoticeResponse.from(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "공지사항 수정")
    public NoticeResponse update(@PathVariable Long id, @Valid @RequestBody UpdateNoticeRequest req) {
        SiteNotice updated = noticeService.update(id, req.displayType(), req.title(), req.body(), req.active());
        return NoticeResponse.from(updated);
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "공지사항 활성/비활성 토글")
    public NoticeResponse setActive(@PathVariable Long id, @RequestBody UpdateNoticeActiveRequest req) {
        return NoticeResponse.from(noticeService.setActive(id, req.active()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "공지사항 삭제")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
