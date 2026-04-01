package com.sqldpass.persistent.question;

import java.util.ArrayList;
import java.util.List;

import com.sqldpass.persistent.common.BaseTimeEntity;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "question", indexes = {
    @Index(name = "idx_question_subject_id", columnList = "subject_id")
})
public class QuestionEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectEntity subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionOptionEntity> options = new ArrayList<>();

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ExplanationEntity explanation;

    public QuestionEntity(SubjectEntity subject, String content) {
        this.subject = subject;
        this.content = content;
    }

    public void addOption(QuestionOptionEntity option) {
        this.options.add(option);
    }

    public void setExplanation(ExplanationEntity explanation) {
        this.explanation = explanation;
    }
}
