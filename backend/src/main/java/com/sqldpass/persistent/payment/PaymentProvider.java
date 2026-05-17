package com.sqldpass.persistent.payment;

/**
 * 결제 채널 — 어떤 PG/스토어를 거쳐 결제됐는지.
 * - PORTONE: 웹 사용자 → PortOne(코리아포트원) 결제창. 카카오페이/카드 등.
 * - PLAY_BILLING: 네이티브 안드로이드 앱 → Google Play 인앱 결제. Play 정책상 디지털 콘텐츠는 강제.
 */
public enum PaymentProvider {
    PORTONE,
    PLAY_BILLING
}
