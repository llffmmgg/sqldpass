package com.sqldpass.domain.member;

import lombok.Getter;

@Getter
public class Member {

    private final Long id;
    private final String provider;
    private final String providerId;
    private final String nickname;
    private final String email;
    private final String profileImage;

    public Member(Long id, String provider, String providerId, String nickname, String email, String profileImage) {
        this.id = id;
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
        this.email = email;
        this.profileImage = profileImage;
    }
}
