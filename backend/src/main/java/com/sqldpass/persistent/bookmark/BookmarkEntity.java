package com.sqldpass.persistent.bookmark;

import com.sqldpass.persistent.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 문제 즐겨찾기. (member_id, question_id) 유니크 — 한 사용자가 같은 문제를 중복
 * 즐겨찾기할 수 없다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "bookmark",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bookmark_member_question",
                        columnNames = {"member_id", "question_id"})
        },
        indexes = {
                @Index(name = "idx_bookmark_member_id", columnList = "member_id")
        }
)
public class BookmarkEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    public BookmarkEntity(Long memberId, Long questionId) {
        this.memberId = memberId;
        this.questionId = questionId;
    }
}
