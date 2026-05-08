package com.sqldpass.service.content;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.controller.content.dto.ContentSnapshotResponse;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ContentSnapshotServiceTest {

    @Mock private MockExamRepository mockExamRepository;
    @Mock private QuestionRepository questionRepository;

    @InjectMocks private ContentSnapshotService service;

    @Test
    @DisplayName("currentVersion combines max(updatedAt) with both counts")
    void currentVersionUsesMaxAndCounts() {
        LocalDateTime mockExamMax = LocalDateTime.of(2026, 1, 5, 10, 0);
        LocalDateTime questionMax = LocalDateTime.of(2026, 2, 1, 12, 30);
        given(mockExamRepository.findSnapshotMaxUpdatedAt()).willReturn(Optional.of(mockExamMax));
        given(questionRepository.findSnapshotMaxUpdatedAt()).willReturn(Optional.of(questionMax));
        given(mockExamRepository.count()).willReturn(40L);
        given(questionRepository.count()).willReturn(1500L);

        String version = service.currentVersion();

        // Question 쪽이 더 최신이므로 그게 prefix
        assertThat(version).startsWith(questionMax.toString());
        assertThat(version).endsWith("-40-1500");
    }

    @Test
    @DisplayName("currentVersion handles empty DB without NPE")
    void currentVersionWithEmptyDb() {
        given(mockExamRepository.findSnapshotMaxUpdatedAt()).willReturn(Optional.empty());
        given(questionRepository.findSnapshotMaxUpdatedAt()).willReturn(Optional.empty());
        given(mockExamRepository.count()).willReturn(0L);
        given(questionRepository.count()).willReturn(0L);

        String version = service.currentVersion();

        assertThat(version).isEqualTo("1970-01-01T00:00-0-0");
    }

    @Test
    @DisplayName("buildSnapshot maps mock exams + questions + subjects to DTO graph")
    void buildSnapshotMapsEntireGraph() {
        SubjectEntity sqldRoot = new SubjectEntity(null, "SQLD", 0);
        setId(SubjectEntity.class, sqldRoot, 100L);
        SubjectEntity sqldChild = new SubjectEntity(sqldRoot, "1과목 데이터 모델링", 0);
        setId(SubjectEntity.class, sqldChild, 101L);

        QuestionEntity q1 = new QuestionEntity(sqldChild, "문제1 본문", 2, "해설1");
        setId(QuestionEntity.class, q1, 1001L);
        q1.assignToMockExam(stub("회차1", ExamType.SQLD, 1, MockExamVisibility.PUBLISHED, true), 1);
        QuestionEntity q2 = new QuestionEntity(sqldChild, "문제2 본문", 3, "해설2");
        setId(QuestionEntity.class, q2, 1002L);

        MockExamEntity exam = stub("회차1", ExamType.SQLD, 1, MockExamVisibility.PUBLISHED, true);
        setId(MockExamEntity.class, exam, 11L);
        exam.linkQuestion(q1, 1);
        exam.linkQuestion(q2, 2);

        given(mockExamRepository.findAllForSnapshot()).willReturn(List.of(exam));
        given(mockExamRepository.findSnapshotMaxUpdatedAt())
                .willReturn(Optional.of(LocalDateTime.of(2026, 1, 1, 0, 0)));
        given(questionRepository.findSnapshotMaxUpdatedAt())
                .willReturn(Optional.of(LocalDateTime.of(2026, 1, 2, 0, 0)));
        given(mockExamRepository.count()).willReturn(1L);
        given(questionRepository.count()).willReturn(2L);

        ContentSnapshotResponse snapshot = service.buildSnapshot();

        assertThat(snapshot.mockExamCount()).isEqualTo(1);
        assertThat(snapshot.questionCount()).isEqualTo(2);
        assertThat(snapshot.version()).contains("-1-2");

        ContentSnapshotResponse.MockExamSnapshot examDto = snapshot.mockExams().get(0);
        assertThat(examDto.id()).isEqualTo(11L);
        assertThat(examDto.examType()).isEqualTo("SQLD");
        assertThat(examDto.visibility()).isEqualTo("PUBLISHED");
        assertThat(examDto.questions()).hasSize(2);

        ContentSnapshotResponse.QuestionSnapshot first = examDto.questions().get(0);
        assertThat(first.displayOrder()).isEqualTo(1);
        assertThat(first.subjectId()).isEqualTo(101L);
        assertThat(first.subjectParentName()).isEqualTo("SQLD");
        assertThat(first.correctOption()).isEqualTo(2);
        assertThat(first.questionType()).isEqualTo("MCQ");
    }

    @Test
    @DisplayName("buildSnapshot orders questions by displayOrder ascending")
    void buildSnapshotOrdersQuestionsByDisplayOrder() {
        SubjectEntity subj = new SubjectEntity(null, "Subj", 0);
        setId(SubjectEntity.class, subj, 200L);
        MockExamEntity exam = stub("E1", ExamType.SQLD, 1, MockExamVisibility.PUBLISHED, true);
        setId(MockExamEntity.class, exam, 21L);

        QuestionEntity q3 = new QuestionEntity(subj, "C", 1, "");
        QuestionEntity q1 = new QuestionEntity(subj, "A", 1, "");
        QuestionEntity q2 = new QuestionEntity(subj, "B", 1, "");
        // 일부러 역순으로 link
        exam.linkQuestion(q3, 3);
        exam.linkQuestion(q1, 1);
        exam.linkQuestion(q2, 2);

        given(mockExamRepository.findAllForSnapshot()).willReturn(List.of(exam));
        given(mockExamRepository.findSnapshotMaxUpdatedAt()).willReturn(Optional.empty());
        given(questionRepository.findSnapshotMaxUpdatedAt()).willReturn(Optional.empty());
        given(mockExamRepository.count()).willReturn(1L);
        given(questionRepository.count()).willReturn(3L);

        ContentSnapshotResponse snapshot = service.buildSnapshot();

        List<ContentSnapshotResponse.QuestionSnapshot> qs = snapshot.mockExams().get(0).questions();
        assertThat(qs).extracting(ContentSnapshotResponse.QuestionSnapshot::content)
                .containsExactly("A", "B", "C");
    }

    private static MockExamEntity stub(String name, ExamType type, int seq,
                                       MockExamVisibility visibility, boolean expertVerified) {
        MockExamEntity e = new MockExamEntity(name, type, seq);
        if (visibility != null) e.changeVisibility(visibility);
        if (expertVerified) e.toggleExpertVerified();
        return e;
    }

    private static void setId(Class<?> clazz, Object entity, Long id) {
        try {
            var field = clazz.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unused")
    private static LocalDate sample() {
        return LocalDate.of(2026, 1, 1);
    }
}
