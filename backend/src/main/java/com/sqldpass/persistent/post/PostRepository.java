package com.sqldpass.persistent.post;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sqldpass.persistent.mockexam.ExamType;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

    /** 게시글 목록 공개 조회. status/category/cert 필터를 적용한다. */
    @Query("SELECT p FROM PostEntity p JOIN FETCH p.member " +
            "WHERE p.status = :status AND p.category = :category " +
            "AND (:cert IS NULL OR p.certKey = :cert) " +
            "ORDER BY p.createdAt DESC")
    Page<PostEntity> findPublic(@Param("status") PostStatus status,
                                @Param("category") PostCategory category,
                                @Param("cert") ExamType cert,
                                Pageable pageable);

    /** 어드민 승인 대기 목록. */
    @Query("SELECT p FROM PostEntity p JOIN FETCH p.member " +
            "WHERE p.status = :status " +
            "ORDER BY p.createdAt DESC")
    List<PostEntity> findByStatusOrderByCreatedAtDesc(@Param("status") PostStatus status);

    /** 어드민 전체 게시글 목록. */
    @Query("SELECT p FROM PostEntity p JOIN FETCH p.member " +
            "ORDER BY p.createdAt DESC")
    List<PostEntity> findAllForAdmin();

    /** 상세 조회용 member fetch. comments 는 별도 쿼리로 가져온다. */
    @Query("SELECT p FROM PostEntity p JOIN FETCH p.member WHERE p.id = :id")
    Optional<PostEntity> findByIdWithMember(@Param("id") Long id);

    /** 조회수 +1. managed entity 를 dirty 로 만들지 않고 응답값만 별도로 보정한다. */
    @Modifying
    @Query("UPDATE PostEntity p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    int incrementViewCount(@Param("id") Long id);

    /** sitemap 용 PUBLISHED 게시글 ID + updatedAt(lastmod). */
    @Query("SELECT p.id AS id, p.updatedAt AS updatedAt FROM PostEntity p " +
            "WHERE p.status = com.sqldpass.persistent.post.PostStatus.PUBLISHED " +
            "ORDER BY p.updatedAt DESC")
    List<PostSeoSummary> findPublishedSeoSummary();

    interface PostSeoSummary {
        Long getId();
        java.time.LocalDateTime getUpdatedAt();
    }
}
