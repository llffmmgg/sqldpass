-- payment.provider 컬럼이 VARCHAR enum mapping 인 경우 별도 ALTER 불필요 (JPA EnumType.STRING).
-- 만약 DB enum 타입이면 ALTER TYPE 필요하지만 sqldpass 는 VARCHAR + JPA enum string 매핑.
-- 본 마이그레이션은 변경 사항 부재를 명시적으로 표시 (forward-only 정책).

-- noop: PaymentProvider.APP_STORE 가 Java enum 에 추가됐을 뿐 DB 컬럼 변경 없음.
SELECT 1;
