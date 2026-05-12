package com.sqldpass.controller.admin;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.persistent.payment.PaymentProvider;
import com.sqldpass.persistent.payment.PaymentRepository;
import com.sqldpass.persistent.payment.PaymentStatus;
import com.sqldpass.service.payment.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * 관리자 결제 — 환불 endpoint.
 *
 * <p>Play Billing 환불은 RTDN 자동 처리이므로 본 컨트롤러는 PortOne 채널 전용.
 */
/**
 * 관리자 결제 — 환불 / 재발급 / 목록 조회.
 *
 * <p>인증 보호는 {@link com.sqldpass.config.AdminAuthInterceptor} 가
 * {@code /api/admin/**} 경로에 일괄 적용. AdminAuthInterceptor 는 admin 토큰을 validate
 * 만 하고 {@code memberId} attribute 는 set 하지 않는다(admin 토큰의 subject 는 username
 * 문자열이라 {@link com.sqldpass.service.admin.JwtProvider#extractMemberId} 가 동작 안 함).
 * 따라서 본 컨트롤러의 {@code actorAdminId} 는 null 일 수 있다 —
 * {@code subscription_history.actor_admin_id} 가 nullable (V81) 이라 저장 안전, 운영 추적은
 * history.reason 텍스트로 보존한다.
 */
@Tag(name = "관리자 결제", description = "관리자 환불·재발급 등")
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    /** 결제 목록 페이지 사이즈 상한 — 메모리/렌더 부담 방어. */
    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping
    @Operation(summary = "결제 목록 (어드민) — 페이지네이션 + status/provider/nickname/paymentId LIKE 필터")
    public Page<AdminPaymentRow> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentProvider provider,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String paymentId,
            HttpServletRequest request) {
        String nick = (nickname == null || nickname.isBlank()) ? null : nickname.trim();
        String pid = (paymentId == null || paymentId.isBlank()) ? null : paymentId.trim();
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return paymentRepository.findAdminPage(status, provider, nick, pid,
                PageRequest.of(safePage, safeSize));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "PortOne 결제 환불 (관리자) — PG 취소 + 구독 회수 + history REFUNDED")
    public RefundResponse refund(@PathVariable Long paymentId,
                                 @Valid @RequestBody RefundRequest body,
                                 HttpServletRequest request) {
        Long actorAdminId = (Long) request.getAttribute("memberId");
        paymentService.revokePortOnePayment(paymentId, body.reason(), actorAdminId);
        return new RefundResponse(paymentId, "refunded");
    }

    @PostMapping("/{paymentId}/reissue-subscription")
    @Operation(summary = "결제 후 구독 미발급 복구 — PAID 결제로 SubscriptionEntity 강제 재발급")
    public ReissueResponse reissue(@PathVariable Long paymentId,
                                   @RequestBody(required = false) ReissueRequest body,
                                   HttpServletRequest request) {
        Long actorAdminId = (Long) request.getAttribute("memberId");
        // body 의 reason 은 운영 편의용 — actorAdminId + 자동 생성 reason 으로 audit 충분.
        PaymentService.ReissueResult result =
                paymentService.reissueSubscription(paymentId, actorAdminId);
        return new ReissueResponse(paymentId, result.issued(), result.expiresAt());
    }

    public record RefundRequest(
            @NotBlank(message = "환불 사유는 필수입니다.")
            @Size(max = 200, message = "환불 사유는 200자 이내로 입력해주세요.")
            String reason
    ) {}
    public record RefundResponse(Long paymentId, String status) {}
    public record ReissueRequest(String reason) {}
    public record ReissueResponse(Long paymentId, boolean issued, LocalDateTime expiresAt) {}
}
