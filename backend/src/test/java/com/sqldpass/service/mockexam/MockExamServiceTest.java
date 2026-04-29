package com.sqldpass.service.mockexam;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.controller.admin.dto.ManualMockExamRequest;
import com.sqldpass.controller.admin.dto.ManualMockExamRequest.ManualQuestion;
import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MockExamServiceTest {

    @Mock
    private MockExamRepository mockExamRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private MockExamCreator mockExamCreator;

    @Mock
    private EngineerMockExamCreator engineerMockExamCreator;

    @Mock
    private ComputerLiteracyMockExamCreator computerLiteracyMockExamCreator;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private MockExamService mockExamService;

    @Test
    @DisplayName("getAll maps grouped rows to summary domain objects")
    void getAll() {
        MockExamEntity entity = new MockExamEntity("Mock Exam 1", ExamType.SQLD, 1);
        setId(entity, 1L);
        given(mockExamRepository.findAllWithQuestionCounts())
                .willReturn(List.<Object[]>of(new Object[]{entity, 50L, 2.0, 1, 4}));

        List<MockExam> result = mockExamService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTotalQuestions()).isEqualTo(50);
        assertThat(result.get(0).getAvgDifficulty()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("create uses the SQLD creator by default")
    void create_defaultSqld() {
        MockExamEntity created = new MockExamEntity("Mock Exam 1", ExamType.SQLD, 1);
        setId(created, 1L);

        given(mockExamCreator.create(isNull())).willReturn(created);
        given(mockExamRepository.findByIdWithQuestions(1L)).willReturn(Optional.of(created));

        MockExam result = mockExamService.create(null, null, null);

        then(mockExamCreator).should().create(null);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getExamType()).isEqualTo(ExamType.SQLD);
    }

    @Test
    @DisplayName("delete releases questions then deletes the mock exam")
    void delete() {
        given(mockExamRepository.existsById(1L)).willReturn(true);

        mockExamService.delete(1L);

        then(questionRepository).should().releaseFromMockExam(1L);
        then(mockExamRepository).should().deleteById(1L);
    }

    @Test
    @DisplayName("delete throws when the mock exam does not exist")
    void delete_notFound() {
        given(mockExamRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> mockExamService.delete(99L))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MOCK_EXAM_NOT_FOUND);
    }

    @Test
    @DisplayName("createManual saves new mock exam with manual MCQ questions")
    void createManual_happyPath() {
        SubjectEntity subject = new SubjectEntity(null, "SQL 활용", 0);
        setSubjectId(subject, 6L);

        given(mockExamRepository.findMaxSequenceByExamType(ExamType.SQLD))
                .willReturn(Optional.of(57));
        given(subjectRepository.findById(6L)).willReturn(Optional.of(subject));

        org.mockito.ArgumentCaptor<MockExamEntity> examCaptor =
                org.mockito.ArgumentCaptor.forClass(MockExamEntity.class);
        given(mockExamRepository.save(examCaptor.capture())).willAnswer(inv -> {
            MockExamEntity e = inv.getArgument(0);
            setId(e, 100L);
            return e;
        });
        given(questionRepository.save(org.mockito.ArgumentMatchers.any(QuestionEntity.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // findByIdWithQuestions 가 마지막에 호출되어 도메인 변환 — 실제 entity 그대로 반환
        given(mockExamRepository.findByIdWithQuestions(100L))
                .willAnswer(inv -> Optional.of(examCaptor.getValue()));

        ManualMockExamRequest request = new ManualMockExamRequest(
                "SQLD 수동 등록",
                ExamType.SQLD,
                false, null, null, null,
                true,
                List.of(
                        new ManualQuestion(6L, "문제1", null, 2, null, null, "해설1", null, null, 2),
                        new ManualQuestion(6L, "문제2", null, 3, null, null, "해설2", null, null, 3)
                )
        );

        MockExam result = mockExamService.createManual(request);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getExamType()).isEqualTo(ExamType.SQLD);
        MockExamEntity captured = examCaptor.getValue();
        assertThat(captured.getName()).isEqualTo("SQLD 수동 등록");
        assertThat(captured.getSequence()).isEqualTo(58);
        assertThat(captured.isExpertVerified()).isTrue();
        assertThat(captured.getQuestions()).hasSize(2);
    }

    @Test
    @DisplayName("createManual rejects when MCQ correctOption is missing")
    void createManual_missingCorrectOption() {
        SubjectEntity subject = new SubjectEntity(null, "SQL 활용", 0);
        setSubjectId(subject, 6L);
        given(mockExamRepository.findMaxSequenceByExamType(ExamType.SQLD)).willReturn(Optional.of(0));
        given(subjectRepository.findById(6L)).willReturn(Optional.of(subject));
        given(mockExamRepository.save(org.mockito.ArgumentMatchers.any(MockExamEntity.class)))
                .willAnswer(inv -> inv.getArgument(0));

        ManualMockExamRequest request = new ManualMockExamRequest(
                "SQLD 수동", ExamType.SQLD, false, null, null, null, false,
                List.of(new ManualQuestion(6L, "문제", null, null, null, null, "해설", null, null, 2))
        );

        assertThatThrownBy(() -> mockExamService.createManual(request))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private static void setSubjectId(SubjectEntity entity, Long id) {
        try {
            var field = SubjectEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setId(MockExamEntity entity, Long id) {
        try {
            var field = MockExamEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
