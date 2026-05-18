package com.sqldpass.controller.payment;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.sqldpass.controller.payment.dto.AppStoreVerifyRequest;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.payment.PaymentProperties;
import com.sqldpass.service.payment.PaymentService;
import com.sqldpass.service.payment.PaymentService.PreparePaymentResult;
import com.sqldpass.service.payment.PaymentService.PreviewResult;
import com.sqldpass.service.payment.PaymentService.VerifyPaymentResult;
import com.sqldpass.service.payment.SubscriptionService;
import com.sqldpass.service.payment.SubscriptionService.ActiveSubscription;
import com.sqldpass.service.setting.AppSettingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * 구독 결제 (PortOne).
 *
 * 흐름:
 *   1. POST /prepare {plan} → paymentId/amount/productName 발급
 *   2. client 가 PortOne SDK 로 결제창 호출
 *   3. POST /verify {paymentId} → PortOne 재검증 → SubscriptionEntity 발급
 *   4. GET /subscription → 활성 구독 정보 조회
 */
@Tag(name = "결제", description = "PortOne 구독 결제")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentProperties paymentProperties;
    private final MemberRepository memberRepository;
    private final SubscriptionService subscriptionService;
    private final AppSettingService appSettingService;

    @GetMapping("/eligibility")
    @Operation(summary = "결제 페이지 접근 가능 여부 — admin 토글 ON 시 전원 허용, OFF 시 화이트리스트 폴백")
    public EligibilityResponse eligibility(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        if (appSettingService.isCheckoutOpenToAll()) {
            return new EligibilityResponse(true);
        }
        var allowed = paymentProperties.reviewerNicknameSet();
        if (allowed.isEmpty()) {
            return new EligibilityResponse(true);
        }
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));
        return new EligibilityResponse(allowed.contains(member.getNickname()));
    }

    @GetMapping("/preview")
    @Operation(summary = "결제 미리 보기 — 활성 구독 prorate 차감 적용된 실 결제 금액 반환")
    public PreviewResult preview(@RequestParam SubscriptionPlan plan, HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        return paymentService.preview(memberId, plan);
    }

    @PostMapping("/prepare")
    @Operation(summary = "결제 사전 등록 — plan + buyer 정보(이름/이메일/휴대폰) 받아 paymentId 발급")
    public PreparePaymentResult prepare(@Valid @RequestBody PrepareRequest body, HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        if (body == null || body.plan() == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "plan 은 필수입니다.");
        }
        return paymentService.prepare(memberId, body.plan(),
                body.buyerName(), body.buyerEmail(), body.buyerPhoneNumber());
    }

    @PostMapping("/verify")
    @Operation(summary = "결제 검증 — PortOne REST 재검증 후 SubscriptionEntity 발급")
    public VerifyPaymentResult verify(@RequestBody VerifyRequest body, HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        if (body == null || body.paymentId() == null || body.paymentId().isBlank()) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "paymentId 는 필수입니다.");
        }
        return paymentService.verify(memberId, body.paymentId());
    }

    @PostMapping("/play-billing/verify")
    @Operation(summary = "Play Billing 영수증 검증 — 안드로이드 앱 전용. 동일 token 재요청은 idempotent.")
    public VerifyPaymentResult verifyPlayBilling(@RequestBody PlayBillingVerifyRequest body,
                                                 HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        if (body == null || body.productId() == null || body.purchaseToken() == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "productId 와 purchaseToken 은 필수입니다.");
        }
        return paymentService.verifyPlayBilling(memberId, body.productId(), body.purchaseToken());
    }

    @PostMapping("/apple/verify")
    @Operation(summary = "App Store 영수증 검증 — iOS 앱 전용. 동일 transactionId 재요청은 idempotent.")
    public VerifyPaymentResult verifyAppStore(@RequestBody AppStoreVerifyRequest body,
                                              HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        if (body == null || body.signedTransaction() == null || body.productId() == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "signedTransaction 과 productId 는 필수입니다.");
        }
        return paymentService.verifyAppStore(memberId, body.signedTransaction(), body.productId());
    }

    @GetMapping("/subscription")
    @Operation(summary = "내 활성 구독 조회 — 없으면 active=false")
    public SubscriptionResponse subscription(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        return subscriptionService.getActive(memberId)
                .map(SubscriptionResponse::from)
                .orElseGet(SubscriptionResponse::inactive);
    }

    /**
     * 결제 사전 등록 요청.
     *
     * <p>buyer 3필드(이름/이메일/휴대폰)는 KG이니시스(신용카드) 결제에 필수이지만 카카오페이는
     * customer 정보를 PG 자체에서 가져오므로 클라이언트가 null/생략 가능하다. 그래서 record
     * 자체에는 @NotBlank 를 두지 않고, 값이 들어왔을 때만 형식 검증(@Email/@Pattern/@Size)을
     * 수행한다. 결제 method 분기는 클라이언트가 담당.
     */
    public record PrepareRequest(
            @NotNull SubscriptionPlan plan,
            @Size(max = 50) String buyerName,
            @Email @Size(max = 255) String buyerEmail,
            @Pattern(regexp = "^01[0-9][-\\s]?\\d{3,4}[-\\s]?\\d{4}$",
                     message = "휴대폰 번호 형식이 올바르지 않습니다.")
            String buyerPhoneNumber
    ) {}

    public record VerifyRequest(String paymentId) {}

    public record PlayBillingVerifyRequest(String productId, String purchaseToken) {}

    public record EligibilityResponse(boolean eligible) {}

    public record SubscriptionResponse(
            boolean active,
            SubscriptionPlan plan,
            java.time.LocalDateTime expiresAt,
            boolean removesAds,
            boolean allowsPdf,
            boolean hasLibraryAccess,
            boolean allowsPremium
    ) {
        public static SubscriptionResponse from(ActiveSubscription a) {
            return new SubscriptionResponse(true, a.plan(), a.expiresAt(),
                    a.removesAds(), a.allowsPdf(), a.hasLibraryAccess(), a.allowsPremium());
        }

        public static SubscriptionResponse inactive() {
            return new SubscriptionResponse(false, null, null, false, false, false, false);
        }
    }
}
