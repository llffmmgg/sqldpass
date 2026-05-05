package com.sqldpass.service.payment;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
import com.sqldpass.persistent.payment.MockExamPurchaseEntity;
import com.sqldpass.persistent.payment.MockExamPurchaseRepository;
import com.sqldpass.persistent.payment.PaymentEntity;
import com.sqldpass.persistent.payment.PaymentRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PortOneClient portOneClient;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private MockExamPurchaseRepository purchaseRepository;
    @Mock
    private com.sqldpass.persistent.mockexam.MockExamRepository mockExamRepository;
    @Mock
    private MemberRepository memberRepository;

    private PaymentProperties properties;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        properties.setReviewerNicknames("pay-rv-7f2a91");
        properties.setDefaultAmount(3000);
        properties.setDefaultProductName("문어CBT 프리미엄 모의고사 1회차 잠금 해제");
        service = new PaymentService(properties, portOneClient,
                paymentRepository, purchaseRepository, mockExamRepository, memberRepository);
    }

    @Test
    @DisplayName("화이트리스트 비어있으면 정식 오픈 모드 — 어떤 회원이든 prepare 통과")
    void prepareAllowsEveryoneWhenWhitelistEmpty() {
        properties.setReviewerNicknames("");

        var result = service.prepare(1L, null);

        assertThat(result.amount()).isEqualTo(3000);
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("화이트리스트에 없는 닉네임이면 prepare 시 PAYMENT_REVIEWER_ONLY")
    void prepareBlocksNonReviewer() {
        MemberEntity m = newMember(1L, "normal-user");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));
        assertThatThrownBy(() -> service.prepare(1L, null))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_REVIEWER_ONLY);
    }

    @Test
    @DisplayName("화이트리스트 닉네임 + null mockExamId 이면 prepare 성공")
    void prepareSucceedsForReviewer() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        var result = service.prepare(1L, null);

        assertThat(result.amount()).isEqualTo(3000);
        assertThat(result.productName()).isEqualTo("문어CBT 프리미엄 모의고사 1회차 잠금 해제");
        assertThat(result.paymentId()).startsWith("sqldpass-");
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("PREMIUM 이 아닌 모의고사로 prepare 호출하면 INVALID_INPUT")
    void prepareRejectsNonPremiumExam() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));
        MockExamEntity exam = newExam(10L, MockExamVisibility.PUBLISHED);
        given(mockExamRepository.findById(10L)).willReturn(Optional.of(exam));

        assertThatThrownBy(() -> service.prepare(1L, 10L))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("verify: PortOne 에서 받은 amount 가 prepare 와 다르면 PAYMENT_AMOUNT_MISMATCH")
    void verifyDetectsAmountTampering() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "상품", 3000);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        // PortOne 응답은 9999원으로 위변조됨
        var info = new PortOneClient.PortOnePaymentInfo("p-1", "PAID", 9999, "KRW",
                OffsetDateTime.now(), Map.of("id", "p-1", "status", "PAID"));
        given(portOneClient.getPayment("p-1")).willReturn(info);

        assertThatThrownBy(() -> service.verify(1L, "p-1"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("verify: PortOne status 가 PAID 가 아니면 PAYMENT_VERIFICATION_FAILED")
    void verifyDetectsNonPaidStatus() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "상품", 3000);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        var info = new PortOneClient.PortOnePaymentInfo("p-1", "READY", 3000, "KRW",
                null, Map.of("id", "p-1", "status", "READY"));
        given(portOneClient.getPayment("p-1")).willReturn(info);

        assertThatThrownBy(() -> service.verify(1L, "p-1"))
                .isInstanceOf(SqldpassException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_VERIFICATION_FAILED);
    }

    @Test
    @DisplayName("verify 성공 시 결제 row 가 PAID 로 마킹되고 mockExamId 가 있으면 잠금 해제 row 가 생성된다")
    void verifySucceedsAndCreatesUnlock() {
        MemberEntity m = newMember(1L, "pay-rv-7f2a91");
        given(memberRepository.findById(1L)).willReturn(Optional.of(m));

        PaymentEntity entity = new PaymentEntity("p-1", 1L, 10L, "상품", 3000);
        given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

        var info = new PortOneClient.PortOnePaymentInfo("p-1", "PAID", 3000, "KRW",
                OffsetDateTime.now(), Map.of("id", "p-1", "status", "PAID"));
        given(portOneClient.getPayment("p-1")).willReturn(info);
        given(purchaseRepository.existsByMemberIdAndMockExamId(1L, 10L)).willReturn(false);

        var result = service.verify(1L, "p-1");

        assertThat(result.paymentId()).isEqualTo("p-1");
        assertThat(result.mockExamId()).isEqualTo(10L);
        ArgumentCaptor<MockExamPurchaseEntity> captor = ArgumentCaptor.forClass(MockExamPurchaseEntity.class);
        verify(purchaseRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(1L);
        assertThat(captor.getValue().getMockExamId()).isEqualTo(10L);
    }

    private MemberEntity newMember(Long id, String nickname) {
        MemberEntity m = new MemberEntity("google", "g-" + id, nickname);
        setField(m, "id", id);
        return m;
    }

    private MockExamEntity newExam(Long id, MockExamVisibility visibility) {
        MockExamEntity e = new MockExamEntity("name", ExamType.SQLD, 1, null);
        setField(e, "id", id);
        setField(e, "visibility", visibility);
        return e;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> cls = target.getClass();
            Field f = null;
            while (cls != null && f == null) {
                try {
                    f = cls.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                    cls = cls.getSuperclass();
                }
            }
            if (f == null) throw new NoSuchFieldException(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
