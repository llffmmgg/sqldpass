package com.sqldpass.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.post.dto.PostDtos.PostDetailResponse;
import com.sqldpass.controller.post.dto.PostDtos.PostSummaryResponse;
import com.sqldpass.service.post.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "어드민-게시판", description = "어드민 — 게시글 승인/삭제")
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostService postService;

    @GetMapping("/pending")
    @Operation(summary = "승인 대기 중인 게시글 (PENDING) 최신 제출순")
    public List<PostSummaryResponse> listPending() {
        return postService.listPending();
    }

    @GetMapping("/{id}")
    @Operation(summary = "게시글 상세 (어드민 — 상태 무관)")
    public PostDetailResponse get(@PathVariable Long id) {
        return postService.getForAdmin(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "게시글 승인 (PENDING → PUBLISHED)")
    public void approve(@PathVariable Long id) {
        postService.approve(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "게시글 삭제 (반려 또는 운영자 직권)")
    public void delete(@PathVariable Long id) {
        postService.deleteByAdmin(id);
    }
}
