package com.sqldpass.service.pdf;

/**
 * 모의고사가 사용자에게 노출될 상태로 전이됐을 때 발행되는 이벤트.
 *  - expert_verified 가 true 로 토글된 직후
 *  - visibility 가 PUBLISHED 로 변경된 직후
 *
 * 트랜잭션 커밋 이후(@TransactionalEventListener(phase=AFTER_COMMIT)) 에만 처리되므로
 * 리스너 안에서는 항상 최신 상태의 모의고사가 DB 에서 조회 가능하다.
 */
public record MockExamPublishedEvent(Long mockExamId) {}
