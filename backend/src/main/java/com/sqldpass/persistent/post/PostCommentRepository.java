package com.sqldpass.persistent.post;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostCommentRepository extends JpaRepository<PostCommentEntity, Long> {

    @Query("SELECT c FROM PostCommentEntity c JOIN FETCH c.member " +
            "WHERE c.post.id = :postId " +
            "ORDER BY c.createdAt ASC")
    List<PostCommentEntity> findByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId);
}
