package com.sqldpass.persistent.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByPaymentId(String paymentId);

    /** Google Play Billing RTDN webhook 이 token 으로 결제 row 찾을 때 사용. */
    Optional<PaymentEntity> findByPurchaseToken(String purchaseToken);
}
