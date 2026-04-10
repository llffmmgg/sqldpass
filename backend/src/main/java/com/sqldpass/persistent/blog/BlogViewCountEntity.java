package com.sqldpass.persistent.blog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "blog_view_count")
public class BlogViewCountEntity {

    @Id
    @Column(length = 200)
    private String slug;

    @Column(nullable = false)
    private long viewCount;

    public BlogViewCountEntity(String slug) {
        this.slug = slug;
        this.viewCount = 0;
    }

    public void incrementView() {
        this.viewCount++;
    }
}
