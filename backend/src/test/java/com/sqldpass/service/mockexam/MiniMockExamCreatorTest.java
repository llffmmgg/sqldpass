package com.sqldpass.service.mockexam;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamKind;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MiniMockExamCreatorTest {

    @Mock
    private MockExamRepository mockExamRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private MiniMockExamCreator creator;

    @Test
    @DisplayName("createAllFromPool: examType null 이면 INVALID_INPUT")
    void createAllFromPool_nullExamType() {
        assertThatThrownBy(() -> creator.createAllFromPool(null))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("createAllFromPool: 풀이 전혀 없으면 INSUFFICIENT_QUESTIONS")
    void createAllFromPool_emptyPool() {
        SubjectEntity root = newSubject(100L, null, "컴퓨터활용능력 2급 필기");
        SubjectEntity leaf1 = newSubject(101L, root, "컴퓨터 일반");
        SubjectEntity leaf2 = newSubject(102L, root, "스프레드시트 일반");

        given(subjectRepository.findByNameAndParentIsNull("컴퓨터활용능력 2급 필기"))
                .willReturn(Optional.of(root));
        given(subjectRepository.findByNameAndParentId("컴퓨터 일반", 100L))
                .willReturn(Optional.of(leaf1));
        given(subjectRepository.findByNameAndParentId("스프레드시트 일반", 100L))
                .willReturn(Optional.of(leaf2));

        given(questionRepository.findMiniPoolBySubject(any()))
                .willReturn(List.of());

        assertThatThrownBy(() -> creator.createAllFromPool(ExamType.COMPUTER_LITERACY_2))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS);
    }

    @Test
    @DisplayName("createAllFromPool: 컴활 2급 풀(각 leaf 24문) → 3회차 생성 (24/8 = 3), 원본 마킹 호출")
    void createAllFromPool_computerLiteracy2_happyPath() {
        SubjectEntity root = newSubject(100L, null, "컴퓨터활용능력 2급 필기");
        SubjectEntity leaf1 = newSubject(101L, root, "컴퓨터 일반");
        SubjectEntity leaf2 = newSubject(102L, root, "스프레드시트 일반");

        given(subjectRepository.findByNameAndParentIsNull("컴퓨터활용능력 2급 필기"))
                .willReturn(Optional.of(root));
        given(subjectRepository.findByNameAndParentId("컴퓨터 일반", 100L))
                .willReturn(Optional.of(leaf1));
        given(subjectRepository.findByNameAndParentId("스프레드시트 일반", 100L))
                .willReturn(Optional.of(leaf2));

        // 컴활 2급 미니 1회 = 각 leaf 8문 (총 16문). 풀이 24씩 있으면 24/8 = 3회 가능.
        given(questionRepository.findMiniPoolBySubject(eq(101L)))
                .willReturn(stubPool(24, 1000L));
        given(questionRepository.findMiniPoolBySubject(eq(102L)))
                .willReturn(stubPool(24, 2000L));

        given(mockExamRepository.findMaxSequenceByExamType(ExamType.COMPUTER_LITERACY_2))
                .willReturn(Optional.of(5));
        given(mockExamRepository.countByExamTypeAndKind(ExamType.COMPUTER_LITERACY_2, MockExamKind.MINI))
                .willReturn(0L);

        long[] mockIdCounter = {500L};
        given(mockExamRepository.save(any(MockExamEntity.class))).willAnswer(inv -> {
            MockExamEntity e = inv.getArgument(0);
            setId(e, mockIdCounter[0]++);
            return e;
        });
        given(questionRepository.save(any(QuestionEntity.class)))
                .willAnswer(inv -> inv.getArgument(0));

        MiniMockExamCreator.GenerationResult result =
                creator.createAllFromPool(ExamType.COMPUTER_LITERACY_2);

        assertThat(result.createdCount()).isEqualTo(3);
        assertThat(result.createdMockExamIds()).hasSize(3);
        assertThat(result.examType()).isEqualTo(ExamType.COMPUTER_LITERACY_2);

        // 잔여 풀: 각 leaf 24 - 3*8 = 0
        assertThat(result.remainingBySubject())
                .containsEntry(101L, 0L)
                .containsEntry(102L, 0L);

        // 마킹 호출 검증 — 회차 3개 × 16문 = 48 원본 마킹
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass((Class) List.class);
        then(questionRepository).should().markIncludedInMiniInBatch(idsCaptor.capture(), any());
        assertThat(idsCaptor.getValue()).hasSize(48);

        // mockExam.save 도 3회 호출
        then(mockExamRepository).should(org.mockito.Mockito.times(3)).save(any(MockExamEntity.class));
    }

    @Test
    @DisplayName("createAllFromPool: 가장 작은 과목 풀이 회차 수를 결정 (병목 검증)")
    void createAllFromPool_smallestSubjectPoolDeterminesRounds() {
        SubjectEntity root = newSubject(100L, null, "컴퓨터활용능력 2급 필기");
        SubjectEntity leaf1 = newSubject(101L, root, "컴퓨터 일반");
        SubjectEntity leaf2 = newSubject(102L, root, "스프레드시트 일반");

        given(subjectRepository.findByNameAndParentIsNull("컴퓨터활용능력 2급 필기"))
                .willReturn(Optional.of(root));
        given(subjectRepository.findByNameAndParentId("컴퓨터 일반", 100L))
                .willReturn(Optional.of(leaf1));
        given(subjectRepository.findByNameAndParentId("스프레드시트 일반", 100L))
                .willReturn(Optional.of(leaf2));

        // leaf1 은 풀이 충분(40문), leaf2 는 17문만. 17/8 = 2 회 가능. 병목은 leaf2.
        given(questionRepository.findMiniPoolBySubject(eq(101L)))
                .willReturn(stubPool(40, 1000L));
        given(questionRepository.findMiniPoolBySubject(eq(102L)))
                .willReturn(stubPool(17, 2000L));

        given(mockExamRepository.findMaxSequenceByExamType(ExamType.COMPUTER_LITERACY_2))
                .willReturn(Optional.of(0));
        given(mockExamRepository.countByExamTypeAndKind(ExamType.COMPUTER_LITERACY_2, MockExamKind.MINI))
                .willReturn(0L);
        long[] c = {1L};
        given(mockExamRepository.save(any(MockExamEntity.class))).willAnswer(inv -> {
            MockExamEntity e = inv.getArgument(0);
            setId(e, c[0]++);
            return e;
        });
        given(questionRepository.save(any(QuestionEntity.class)))
                .willAnswer(inv -> inv.getArgument(0));

        MiniMockExamCreator.GenerationResult result =
                creator.createAllFromPool(ExamType.COMPUTER_LITERACY_2);

        // 2회만 만들어짐 (leaf2 17문 / 8 = 2)
        assertThat(result.createdCount()).isEqualTo(2);
        // leaf1 잔여 40 - 2*8 = 24, leaf2 잔여 17 - 2*8 = 1
        assertThat(result.remainingBySubject())
                .containsEntry(101L, 24L)
                .containsEntry(102L, 1L);
    }

    // ---- helpers ----

    private static List<QuestionEntity> stubPool(int size, long startId) {
        List<QuestionEntity> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            QuestionEntity q = newQuestionStub();
            setQuestionId(q, startId + i);
            list.add(q);
        }
        return list;
    }

    private static QuestionEntity newQuestionStub() {
        SubjectEntity dummySubject = new SubjectEntity(null, "dummy", 0);
        return new QuestionEntity(dummySubject, "본문", 1, "해설", null, null, 2);
    }

    private static SubjectEntity newSubject(Long id, SubjectEntity parent, String name) {
        SubjectEntity s = new SubjectEntity(parent, name, 0);
        try {
            Field f = SubjectEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(s, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return s;
    }

    private static void setId(MockExamEntity entity, Long id) {
        try {
            Field f = MockExamEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setQuestionId(QuestionEntity q, Long id) {
        try {
            Field f = QuestionEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(q, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
