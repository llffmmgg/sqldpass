package com.sqldpass.persistent.member;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findByProviderAndProviderId(String provider, String providerId);

    Optional<MemberEntity> findByNickname(String nickname);

    /** 어드민 목록에서 더미(provider='SEED') 사용자를 제외 */
    Page<MemberEntity> findByProviderNot(String provider, Pageable pageable);
}
