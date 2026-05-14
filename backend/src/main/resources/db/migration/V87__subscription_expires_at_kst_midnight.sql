-- 새 만료 정책 (subscription-midnight-policy phase) 적용 — 기존 active 구독 일괄 보정.
-- 공식: 결제일(KR) + (plan.days + 1)일의 00:00 KST.
--
-- 운영 LocalDateTime 은 UTC 기준으로 저장되어 있으므로 (+9h) 로 KR 일자 추정 후 자정 정렬.
-- 안전 조건: TIMESTAMPDIFF(HOUR, paid_at/purchased_at, expires_at) 이 정확히 plan.days × 24h 인 row 만 보정.
--   → 기존 정책 흔적이 있는 row 만 대상. 어드민이 수동으로 만료를 늦췄거나 다른 방식으로
--      박힌 row 는 보존. UNLIMITED 는 expires_at NULL 이라 영향 없음.
--
-- 멱등: WHERE 안전 조건이 새 자정 만료에는 매치되지 않으므로 재실행해도 no-op.

-- ============================================================
-- 1) 결제 연결 구독 (paymentId NOT NULL) — payment.paid_at 기준
-- ============================================================
UPDATE subscription s
JOIN payment p ON p.id = s.payment_id
SET s.expires_at = DATE(DATE_ADD(p.paid_at, INTERVAL 9 HOUR)) + INTERVAL (
    CASE s.plan
        WHEN 'THREE_DAY' THEN 4
        WHEN 'FOCUS'     THEN 31
        WHEN 'ONE_MONTH' THEN 31
    END
) DAY
WHERE s.archived_at IS NULL
  AND s.plan IN ('THREE_DAY', 'FOCUS', 'ONE_MONTH')
  AND s.expires_at IS NOT NULL
  AND TIMESTAMPDIFF(HOUR, p.paid_at, s.expires_at) = CASE s.plan
        WHEN 'THREE_DAY' THEN 72
        WHEN 'FOCUS'     THEN 720
        WHEN 'ONE_MONTH' THEN 720
      END;

-- ============================================================
-- 2) 어드민 수동 발급 (paymentId IS NULL) — subscription.purchased_at 기준
-- ============================================================
UPDATE subscription s
SET s.expires_at = DATE(DATE_ADD(s.purchased_at, INTERVAL 9 HOUR)) + INTERVAL (
    CASE s.plan
        WHEN 'THREE_DAY' THEN 4
        WHEN 'FOCUS'     THEN 31
        WHEN 'ONE_MONTH' THEN 31
    END
) DAY
WHERE s.archived_at IS NULL
  AND s.payment_id IS NULL
  AND s.plan IN ('THREE_DAY', 'FOCUS', 'ONE_MONTH')
  AND s.expires_at IS NOT NULL
  AND TIMESTAMPDIFF(HOUR, s.purchased_at, s.expires_at) = CASE s.plan
        WHEN 'THREE_DAY' THEN 72
        WHEN 'FOCUS'     THEN 720
        WHEN 'ONE_MONTH' THEN 720
      END;
