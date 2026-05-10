package com.sqldpass.service.payment;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.payment.PaymentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 결제 실패 상태(markFailed) 를 호출자 트랜잭션과 분리해 별도 트랜잭션에서 flush 한다.
 *
 * PaymentService.verify 가 실패 시 throw 로 끝나면 같은 트랜잭션 안의 markFailed 변경이
 * 롤백되어 status=PENDING 으로 영구 잔존한다. REQUIRES_NEW 로 분리하면 throw 경로에서도
 * 실패 row 가 보존된다.
 *
 * Spring AOP proxy 우회를 막기 위해 별도 컴포넌트로 분리 — PaymentService 내부 self-invocation
 * 으로 호출하면 propagation 속성이 무시된다.
 */
@Component
@RequiredArgsConstructor
public class PaymentFailureRecorder {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedInNewTx(Long paymentEntityId, String pgResponse) {
        paymentRepository.findById(paymentEntityId)
                .ifPresent(p -> p.markFailed(pgResponse));
    }
}
