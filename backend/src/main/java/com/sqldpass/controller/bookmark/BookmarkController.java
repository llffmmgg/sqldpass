package com.sqldpass.controller.bookmark;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.bookmark.dto.BookmarkExistsResponse;
import com.sqldpass.controller.bookmark.dto.BookmarkListResponse;
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
    @Operation(summary = "내 즐겨찾기 목록 (최신순)",
            description = "Thunder/Focus/Pro/Lifetime 회원은 전체 반환. "
                    + "그 외 사용자는 최근 30개만 반환되며 limited=true 로 표시. "
                    + "31번째 이상 즐겨찾기 데이터는 백엔드에 보존되며 결제 후 즉시 복원됨.")
    public BookmarkListResponse list(HttpServletRequest request) {
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
