package com.sqldpass.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.service.payment.AdminSubscriptionService;
import com.sqldpass.service.payment.AdminSubscriptionService.AdminSubscriptionRow;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * 어드민 — 구독 조회/수동 발급/수동 만료.
 */
@Tag(name = "관리자 - 구독", description = "구독권 조회·수동 발급·수동 만료")
@RestController
@RequestMapping("/api/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final AdminSubscriptionService service;

    @GetMapping
    @Operation(summary = "구독 목록 (활성/만료 모두)", description = "nickname=  닉네임 LIKE 검색.")
    public Page<AdminSubscriptionRow> list(
            @RequestParam(required = false) String nickname,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return service.list(nickname, page, size);
    }

    @PostMapping
    @Operation(summary = "구독 수동 발급 (보상·이벤트·환불 재발급 등)")
    public AdminSubscriptionRow grant(@RequestBody GrantRequest body, HttpServletRequest request) {
        Long actorAdminId = (Long) request.getAttribute("memberId");
        return service.grantManual(body.memberId(), body.plan(), body.reason(), actorAdminId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "구독 수동 만료 (강제 종료)")
    public void expire(@PathVariable Long id,
                       @RequestParam(required = false) String reason,
                       HttpServletRequest request) {
        Long actorAdminId = (Long) request.getAttribute("memberId");
        service.expireManual(id, reason, actorAdminId);
    }

    public record GrantRequest(Long memberId, SubscriptionPlan plan, String reason) {}
}
