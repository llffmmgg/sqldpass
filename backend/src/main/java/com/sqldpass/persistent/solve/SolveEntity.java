package com.sqldpass.persistent.solve;

import java.util.ArrayList;
import java.util.List;

import com.sqldpass.persistent.common.BaseTimeEntity;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.subject.SubjectEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "solve", indexes = {
    @Index(name = "idx_solve_member_id", columnList = "member_id")
})
public class SolveEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectEntity subject;

    @Column(nullable = false)
    private int totalCount;

    @Column(nullable = false)
    private int correctCount;

    @Column(nullable = false)
    private int score;

    @OneToMany(mappedBy = "solve", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolveAnswerEntity> answers = new ArrayList<>();

    public SolveEntity(MemberEntity member, SubjectEntity subject, int totalCount, int correctCount, int score) {
        this.member = member;
        this.subject = subject;
        this.totalCount = totalCount;
        this.correctCount = correctCount;
        this.score = score;
    }

    public void addAnswer(SolveAnswerEntity answer) {
        this.answers.add(answer);
    }
}
