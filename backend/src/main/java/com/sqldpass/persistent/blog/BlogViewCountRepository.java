package com.sqldpass.persistent.blog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BlogViewCountRepository extends JpaRepository<BlogViewCountEntity, String> {

    @Modifying
    @Query(value = "INSERT INTO blog_view_count (slug, view_count) VALUES (:slug, 1) " +
            "ON DUPLICATE KEY UPDATE view_count = view_count + 1", nativeQuery = true)
    void incrementViewCount(String slug);
}
