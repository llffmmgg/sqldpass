package com.sqldpass.controller.bookmark.dto;

import java.util.List;

/**
 * 즐겨찾기 목록 응답. 권한이 없는 사용자에게는 30 개만 반환하고 limited=true 로 표시한다.
 * 31번째 추가는 차단하지 않음 — 백엔드는 그대로 저장하고 표시만 잘림. 결제 후 그대로 복원.
 */
public record BookmarkListResponse(
        List<BookmarkResponse> items,
        long totalCount,
        boolean limited,
        int freeLimit
) {}
