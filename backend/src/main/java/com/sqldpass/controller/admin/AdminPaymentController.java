package com.sqldpass.controller.admin;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.payment.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * 관리자 결제 — 환불 endpoint.
 *
 * <p>Play Billing 환불은 RTDN 자동 처리이므로 본 컨트롤러는 PortOne 채널 전용.
 */
@Tag(name = "관리자 결제", description = "관리자 환불·재발급 등")
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "PortOne 결제 환불 (관리자) — PG 취소 + 구독 회수 + history REFUNDED")
    public RefundResponse refund(@PathVariable Long paymentId,
                                 @RequestBody(required = false) RefundRequest body,
                                 HttpServletRequest request) {
        Long actorAdminId = (Long) request.getAttribute("memberId");
        if (actorAdminId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        String reason = body == null ? null : body.reason();
        paymentService.revokePortOnePayment(paymentId, reason, actorAdminId);
        return new RefundResponse(paymentId, "refunded");
    }

    public record RefundRequest(String reason) {}
    public record RefundResponse(Long paymentId, String status) {}
}
