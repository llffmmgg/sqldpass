package com.sqldpass.persistent.post;

import java.util.ArrayList;
import java.util.List;

import com.sqldpass.persistent.common.BaseTimeEntity;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.mockexam.ExamType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "post", indexes = {
        @Index(name = "idx_post_status_category_created", columnList = "status, category, createdAt"),
        @Index(name = "idx_post_cert", columnList = "certKey"),
        @Index(name = "idx_post_member", columnList = "member_id")
})
public class PostEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    /** PASS_REVIEW 카테고리에서만 NOT NULL. 다른 카테고리에서는 NULL. */
    @Enumerated(EnumType.STRING)
    @Column(name = "cert_key", length = 30)
    private ExamType certKey;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private long viewCount;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostCommentEntity> comments = new ArrayList<>();

    public PostEntity(MemberEntity member, PostCategory category, PostStatus status,
                      ExamType certKey, String title, String content) {
        this.member = member;
        this.category = category;
        this.status = status;
        this.certKey = certKey;
        this.title = title;
        this.content = content;
        this.viewCount = 0L;
    }

    public void approve() {
        this.status = PostStatus.PUBLISHED;
    }

    public void edit(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
