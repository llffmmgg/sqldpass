package com.sqldpass.service.payment;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
import com.sqldpass.persistent.payment.MockExamPurchaseEntity;
import com.sqldpass.persistent.payment.MockExamPurchaseRepository;
import com.sqldpass.persistent.payment.PaymentEntity;
import com.sqldpass.persistent.payment.PaymentRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentProperties properties;
    private final PortOneClient portOneClient;
    private final PaymentRepository paymentRepository;
    private final MockExamPurchaseRepository purchaseRepository;
    private final MockExamRepository mockExamRepository;
    private final MemberRepository memberRepository;

    /**
     * 결제 직전 — client 가 PortOne 결제창을 띄우기 전에 paymentId 를 사전 등록해
     * 검증 단계에서 멱등키·금액 위변조 방지에 사용한다.
     *
     * @param mockExamId 잠금 해제 대상. PREMIUM 인지 검증.
     * @return 클라이언트가 PortOne.requestPayment 에 그대로 전달할 paymentId/amount/productName
     */
    @Transactional
    public PreparePaymentResult prepare(Long memberId, Long mockExamId) {
        ensureReviewer(memberId);

        if (mockExamId != null) {
            MockExamEntity exam = mockExamRepository.findById(mockExamId)
                    .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));
            if (exam.getVisibility() != MockExamVisibility.PREMIUM) {
                throw new SqldpassException(ErrorCode.INVALID_INPUT,
                        "PREMIUM 모의고사만 결제 대상입니다.");
            }
            if (purchaseRepository.existsByMemberIdAndMockExamId(memberId, mockExamId)) {
                throw new SqldpassException(ErrorCode.INVALID_INPUT,
                        "이미 잠금 해제된 모의고사입니다.");
            }
        }

        int amount = properties.getDefaultAmount();
        if (amount < 1) {
            // 0원/음수는 카드사 심사 불가 + 보안상 무의미
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED,
                    "결제 금액 설정이 올바르지 않습니다.");
        }
        String productName = properties.getDefaultProductName();
        if (productName == null || productName.isBlank() || productName.toUpperCase().contains("TEST")) {
            // 카드사 심사 가이드: 상품명에 'TEST' 금지
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED,
                    "결제 상품명 설정이 올바르지 않습니다.");
        }

        String paymentId = "sqldpass-" + System.currentTimeMillis() + "-" + memberId;
        PaymentEntity entity = new PaymentEntity(paymentId, memberId, mockExamId, productName, amount);
        paymentRepository.save(entity);

        log.info("결제 prepare memberId={} mockExamId={} paymentId={} amount={}",
                memberId, mockExamId, paymentId, amount);
        return new PreparePaymentResult(
                paymentId,
                amount,
                productName,
                properties.getPortone().getStoreId());
    }

    /**
     * 결제 완료 후 — client 가 PortOne 결제창에서 성공 응답을 받으면 호출.
     * PortOne REST API 로 status/amount 재검증 후 잠금 해제 권리 발급.
     */
    @Transactional
    public VerifyPaymentResult verify(Long memberId, String paymentId) {
        ensureReviewer(memberId);

        PaymentEntity entity = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.PAYMENT_NOT_FOUND));
        if (!entity.getMemberId().equals(memberId)) {
            throw new SqldpassException(ErrorCode.FORBIDDEN, "본인의 결제만 검증할 수 있습니다.");
        }

        PortOneClient.PortOnePaymentInfo info = portOneClient.getPayment(paymentId);
        if (!info.isPaid()) {
            entity.markFailed("status=" + info.status());
            throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED,
                    "PortOne 결제 상태가 PAID 가 아닙니다: " + info.status());
        }
        if (info.amountTotal() != entity.getAmount()) {
            entity.markFailed("expected=" + entity.getAmount() + " actual=" + info.amountTotal());
            throw new SqldpassException(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "결제 금액이 일치하지 않습니다 (expected=" + entity.getAmount()
                            + ", actual=" + info.amountTotal() + ")");
        }

        LocalDateTime paidAt = info.paidAt() != null
                ? info.paidAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                : LocalDateTime.now();
        entity.markPaid(info.raw() == null ? null : info.raw().toString(), paidAt);

        if (entity.getMockExamId() != null
                && !purchaseRepository.existsByMemberIdAndMockExamId(memberId, entity.getMockExamId())) {
            purchaseRepository.save(new MockExamPurchaseEntity(
                    memberId, entity.getMockExamId(), entity.getId(), paidAt));
        }

        log.info("결제 verify 성공 memberId={} paymentId={} mockExamId={}",
                memberId, paymentId, entity.getMockExamId());
        return new VerifyPaymentResult(paymentId, entity.getAmount(), entity.getProductName(),
                entity.getMockExamId());
    }

    private void ensureReviewer(Long memberId) {
        var allowed = properties.reviewerNicknameSet();
        if (allowed.isEmpty()) {
            // 아직 정식 오픈 전 — 화이트리스트 비어있으면 전부 차단해 사고 방지
            throw new SqldpassException(ErrorCode.PAYMENT_REVIEWER_ONLY,
                    "결제는 현재 심사 단계입니다. 잠시 후 다시 시도해주세요.");
        }
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));
        if (!allowed.contains(member.getNickname())) {
            throw new SqldpassException(ErrorCode.PAYMENT_REVIEWER_ONLY);
        }
    }

    public record PreparePaymentResult(String paymentId, int amount, String productName, String storeId) {}

    public record VerifyPaymentResult(String paymentId, int amount, String productName, Long mockExamId) {}
}
