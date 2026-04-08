package com.sqldpass.controller.publicapi.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 랜딩 페이지 노출용 TOP 30 랭킹 응답.
 *
 * - entries: 등수 / 닉네임 / 누적 정답 수
 * - generatedAt: 캐시 생성 시각 (1시간마다 갱신)
 *
 * 닉네임은 unique constraint라 본인 매칭 가능. memberId는 노출하지 않음.
 */
public record PublicRankingResponse(
        List<Entry> entries,
        LocalDateTime generatedAt
) {
    public record Entry(
            int rank,
            String nickname,
            long totalCorrect
    ) {
    }
}
