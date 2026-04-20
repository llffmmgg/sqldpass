package com.sqldpass.controller.streak;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.streak.dto.StreakResponse;
import com.sqldpass.service.streak.StreakService;

import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Streak", description = "연속 학습 조회 API")
@RestController
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;

    @GetMapping("/api/streak/me")
    @Operation(summary = "내 연속 학습 정보 조회")
    public StreakResponse getMyStreak(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        return streakService.getMyStreak(memberId);
    }
}
