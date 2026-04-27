package com.sqldpass.persistent.post;

import com.sqldpass.persistent.common.BaseTimeEntity;
import com.sqldpass.persistent.member.MemberEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "post_comment", indexes = {
        @Index(name = "idx_comment_post_created", columnList = "post_id, createdAt")
})
public class PostCommentEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public PostCommentEntity(PostEntity post, MemberEntity member, String content) {
        this.post = post;
        this.member = member;
        this.content = content;
    }

    public void edit(String content) {
        this.content = content;
    }
}
