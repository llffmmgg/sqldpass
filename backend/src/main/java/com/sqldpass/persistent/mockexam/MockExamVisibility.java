package com.sqldpass.persistent.mockexam;

/**
 * 모의고사 공개 상태.
 *
 * - DRAFT      : 어드민에만 보임. 사용자 목록·무한 풀이 풀에서 제외.
 * - PUBLISHED  : 일반 공개. 사용자 목록 + 무한 풀이 풀 포함.
 * - PREMIUM    : 사용자 목록에는 보이지만 잠금. 무한 풀이 풀에서는 제외.
 *                향후 결제 시스템 연동 후 권한 있는 사용자만 풀이 가능.
 */
public enum MockExamVisibility {
    DRAFT,
    PUBLISHED,
    PREMIUM
}
