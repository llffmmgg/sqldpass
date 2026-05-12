package com.sqldpass.controller.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.service.setting.AppSettingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

/**
 * 어드민 — 런타임 설정 토글.
 *
 * <p>인증 보호는 {@link com.sqldpass.config.AdminAuthInterceptor} 가
 * {@code /api/admin/**} 경로에 일괄 적용.
 */
@Tag(name = "관리자 설정", description = "결제창 노출 등 런타임 토글")
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingController {

    private final AppSettingService appSettingService;

    @GetMapping("/payment/checkout-open")
    @Operation(summary = "결제창 전체 공개 여부 조회")
    public CheckoutOpenResponse getCheckoutOpen() {
        return new CheckoutOpenResponse(appSettingService.isCheckoutOpenToAll());
    }

    @PutMapping("/payment/checkout-open")
    @Operation(summary = "결제창 전체 공개 여부 변경 — false 면 reviewer-nicknames 화이트리스트 폴백")
    public CheckoutOpenResponse setCheckoutOpen(@Valid @RequestBody CheckoutOpenRequest body) {
        appSettingService.setCheckoutOpenToAll(body.openToAll());
        return new CheckoutOpenResponse(appSettingService.isCheckoutOpenToAll());
    }

    public record CheckoutOpenRequest(@NotNull Boolean openToAll) {}

    public record CheckoutOpenResponse(boolean openToAll) {}
}
