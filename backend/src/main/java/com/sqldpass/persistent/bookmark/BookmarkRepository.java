package com.sqldpass.persistent.bookmark;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<BookmarkEntity, Long> {

    boolean existsByMemberIdAndQuestionId(Long memberId, Long questionId);

    void deleteByMemberIdAndQuestionId(Long memberId, Long questionId);

    List<BookmarkEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /** 즐겨찾기된 문제 id만 뽑는다 — BookmarkButton 벌크 상태 체크용. */
    List<Long> findQuestionIdByMemberIdAndQuestionIdIn(Long memberId, List<Long> questionIds);

    long countByMemberId(Long memberId);
}
