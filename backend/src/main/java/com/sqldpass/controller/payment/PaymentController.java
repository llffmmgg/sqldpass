package com.sqldpass.controller.payment;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.payment.PaymentProperties;
import com.sqldpass.service.payment.PaymentService;
import com.sqldpass.service.payment.PaymentService.PreparePaymentResult;
import com.sqldpass.service.payment.PaymentService.VerifyPaymentResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * PortOne 결제 흐름 — 카드사 심사용 최소 구현.
 *
 * 흐름:
 *   1. client 가 POST /api/payment/prepare 로 결제 사전 등록 → paymentId 받기
 *   2. client 가 PortOne SDK 의 requestPayment 로 paymentId·amount·productName 사용해 결제창 띄움
 *   3. client 가 결제 성공 응답을 받으면 POST /api/payment/verify 호출
 *   4. backend 가 PortOne REST 로 status/amount 재검증 후 잠금 해제 권리 발급
 */
@Tag(name = "결제", description = "PortOne 결제 (카드사 심사용 화이트리스트 게이트)")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentProperties paymentProperties;
    private final MemberRepository memberRepository;

    @GetMapping("/eligibility")
    @Operation(summary = "결제 페이지 접근 가능 여부 — 화이트리스트 닉네임만 true")
    public EligibilityResponse eligibility(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        var allowed = paymentProperties.reviewerNicknameSet();
        if (allowed.isEmpty()) {
            return new EligibilityResponse(false);
        }
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));
        return new EligibilityResponse(allowed.contains(member.getNickname()));
    }

    @PostMapping("/prepare")
    @Operation(summary = "결제 사전 등록 — paymentId·amount·productName 발급")
    public PreparePaymentResult prepare(@RequestBody PrepareRequest body, HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        return paymentService.prepare(memberId, body == null ? null : body.mockExamId());
    }

    @PostMapping("/verify")
    @Operation(summary = "결제 검증 — PortOne REST 재검증 후 잠금 해제 권리 발급")
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

    public record PrepareRequest(Long mockExamId) {}

    public record VerifyRequest(String paymentId) {}

    public record EligibilityResponse(boolean eligible) {}
}
