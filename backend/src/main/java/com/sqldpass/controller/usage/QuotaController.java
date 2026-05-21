package com.sqldpass.controller.usage;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.service.usage.DailyUsageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * 무료 일일 한도 조회. 클라이언트가 사전 표시("오늘 18/30") 하는 데 사용.
 * 활성 구독자/비로그인은 limit=null(무제한) 응답.
 */
@Tag(name = "사용량", description = "무료 일일 한도 조회")
@RestController
@RequiredArgsConstructor
public class QuotaController {

    private final DailyUsageService dailyUsageService;

    @GetMapping("/api/quota")
    @Operation(summary = "내 일일 사용량 조회",
            description = "{ questionUsed, questionLimit, mockUsed, mockLimit, resetAt }. " +
                    "활성 구독자는 limit=null (무제한). resetAt 은 KST naive — 프론트가 +09:00 부착.")
    public DailyUsageService.Quota getMyQuota(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        return dailyUsageService.getQuota(memberId);
    }
}
