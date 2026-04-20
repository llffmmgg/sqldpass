package com.sqldpass.controller.bookmark;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.bookmark.dto.BookmarkExistsResponse;
import com.sqldpass.controller.bookmark.dto.BookmarkResponse;
import com.sqldpass.service.bookmark.BookmarkService;

import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "즐겨찾기", description = "문제 즐겨찾기 관련 API")
@RestController
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping("/api/bookmarks")
    @Operation(summary = "내 즐겨찾기 목록 (최신순)")
    public List<BookmarkResponse> list(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        return bookmarkService.list(memberId);
    }

    @PostMapping("/api/bookmarks/{questionId}")
    @Operation(summary = "즐겨찾기 추가 (이미 있으면 멱등성)")
    public ResponseEntity<Void> add(HttpServletRequest request, @PathVariable Long questionId) {
        Long memberId = (Long) request.getAttribute("memberId");
        bookmarkService.add(memberId, questionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/bookmarks/{questionId}")
    @Operation(summary = "즐겨찾기 제거 (없어도 204)")
    public ResponseEntity<Void> remove(HttpServletRequest request, @PathVariable Long questionId) {
        Long memberId = (Long) request.getAttribute("memberId");
        bookmarkService.remove(memberId, questionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/bookmarks/exists/{questionId}")
    @Operation(summary = "특정 문제 즐겨찾기 여부 조회 (버튼 상태 동기화용)")
    public BookmarkExistsResponse exists(HttpServletRequest request, @PathVariable Long questionId) {
        Long memberId = (Long) request.getAttribute("memberId");
        return new BookmarkExistsResponse(bookmarkService.exists(memberId, questionId));
    }
}
