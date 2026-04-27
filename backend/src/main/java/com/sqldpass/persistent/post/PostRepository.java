package com.sqldpass.persistent.post;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sqldpass.persistent.mockexam.ExamType;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

    /** 게시판 목록 — 상태/카테고리/자격증 필터. cert 가 null 이면 전체 자격증. */
    @Query("SELECT p FROM PostEntity p JOIN FETCH p.member " +
            "WHERE p.status = :status AND p.category = :category " +
            "AND (:cert IS NULL OR p.certKey = :cert) " +
            "ORDER BY p.createdAt DESC")
    Page<PostEntity> findPublic(@Param("status") PostStatus status,
                                @Param("category") PostCategory category,
                                @Param("cert") ExamType cert,
                                Pageable pageable);

    /** 어드민 승인 대기 목록 (status=PENDING, 최신 제출순). */
    @Query("SELECT p FROM PostEntity p JOIN FETCH p.member " +
            "WHERE p.status = :status " +
            "ORDER BY p.createdAt DESC")
    List<PostEntity> findByStatusOrderByCreatedAtDesc(@Param("status") PostStatus status);

    /** 어드민 — 모든 status 의 게시글 (최신순). */
    @Query("SELECT p FROM PostEntity p JOIN FETCH p.member " +
            "ORDER BY p.createdAt DESC")
    List<PostEntity> findAllForAdmin();

    /** 상세 — member fetch. comments 는 별도 쿼리로 가져오는 게 안전 (페이지네이션 가능성). */
    @Query("SELECT p FROM PostEntity p JOIN FETCH p.member WHERE p.id = :id")
    Optional<PostEntity> findByIdWithMember(@Param("id") Long id);
}
