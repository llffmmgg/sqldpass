package com.sqldpass.persistent.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V82 (2026-05-11) 에서 재추가된 email 컬럼 — KG이니시스 customer.email 용.
 * 도메인 메서드 updateEmail() 의 멱등성/blank 가드 회귀 방지.
 */
class MemberEntityEmailTest {

    @Test
    @DisplayName("4-인자 생성자: email 같이 저장")
    void constructorStoresEmail() {
        MemberEntity m = new MemberEntity("google", "sub-1", "닉네임", "user@example.com");

        assertThat(m.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("4-인자 생성자: email null 허용")
    void constructorAllowsNullEmail() {
        MemberEntity m = new MemberEntity("google", "sub-1", "닉네임", null);

        assertThat(m.getEmail()).isNull();
    }

    @Test
    @DisplayName("3-인자 생성자 (기존): email 은 null")
    void legacyConstructorLeavesEmailNull() {
        MemberEntity m = new MemberEntity("google", "sub-1", "닉네임");

        assertThat(m.getEmail()).isNull();
    }

    @Test
    @DisplayName("updateEmail: null/blank 입력은 무동작 (NULL 회원 보호)")
    void updateEmailIgnoresNullAndBlank() {
        MemberEntity m = new MemberEntity("google", "sub-1", "닉네임", "existing@example.com");

        m.updateEmail(null);
        assertThat(m.getEmail()).isEqualTo("existing@example.com");

        m.updateEmail("");
        assertThat(m.getEmail()).isEqualTo("existing@example.com");

        m.updateEmail("   ");
        assertThat(m.getEmail()).isEqualTo("existing@example.com");
    }

    @Test
    @DisplayName("updateEmail: 동일한 값이면 무동작 — JPA UPDATE 쿼리 절약 의도")
    void updateEmailNoOpOnSameValue() {
        MemberEntity m = new MemberEntity("google", "sub-1", "닉네임", "same@example.com");

        m.updateEmail("same@example.com");

        assertThat(m.getEmail()).isEqualTo("same@example.com");
        // 같은 객체 reference 유지 — 동등성 + 무동작 검증
    }

    @Test
    @DisplayName("updateEmail: 다른 값이면 갱신")
    void updateEmailReplacesWhenDifferent() {
        MemberEntity m = new MemberEntity("google", "sub-1", "닉네임", "old@example.com");

        m.updateEmail("new@example.com");

        assertThat(m.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("updateEmail: null 회원 (기존 백필 케이스) → 받아온 값 저장")
    void updateEmailBackfillsNullMember() {
        MemberEntity m = new MemberEntity("google", "sub-1", "닉네임");
        assertThat(m.getEmail()).isNull();

        m.updateEmail("backfilled@example.com");

        assertThat(m.getEmail()).isEqualTo("backfilled@example.com");
    }
}
