package com.sqldpass.domain.member;

import lombok.Getter;

@Getter
public class Member {

    private final Long id;
    private final String provider;
    private final String providerId;
    private final String nickname;

    public Member(Long id, String provider, String providerId, String nickname) {
        this.id = id;
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
    }
}
