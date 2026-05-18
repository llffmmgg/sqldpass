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
import com.sqldpass.persistent.mockexam.MockExamVisibility;
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
import static org.mockito.ArgumentMatchers.isNull;
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
    @DisplayName("splitQuotaToSources: quota 를 PAST_EXAM/AI_PUB/AI_PREM 으로 1:1:1 분배 (잔여는 앞부터 +1)")
    void splitQuotaToSources_distribution() {
        // 균등 분배 (3 의 배수)
        assertThat(MiniMockExamCreator.splitQuotaToSources(6))
                .containsExactly(2, 2, 2);
        assertThat(MiniMockExamCreator.splitQuotaToSources(3))
                .containsExactly(1, 1, 1);

        // 잔여 1 → 첫 source 에 +1
        assertThat(MiniMockExamCreator.splitQuotaToSources(7))
                .containsExactly(3, 2, 2);
        assertThat(MiniMockExamCreator.splitQuotaToSources(1))
                .containsExactly(1, 0, 0);

        // 잔여 2 → 첫 두 source 에 +1
        assertThat(MiniMockExamCreator.splitQuotaToSources(8))
                .containsExactly(3, 3, 2);
        assertThat(MiniMockExamCreator.splitQuotaToSources(2))
                .containsExactly(1, 1, 0);

        // 0
        assertThat(MiniMockExamCreator.splitQuotaToSources(0))
                .containsExactly(0, 0, 0);
    }

    @Test
    @DisplayName("createAllFromPool: examType null 이면 INVALID_INPUT")
    void createAllFromPool_nullExamType() {
        assertThatThrownBy(() -> creator.createAllFromPool(null, null))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("createAllFromPool: difficulty 가 1~4 범위 밖이면 INVALID_INPUT")
    void createAllFromPool_invalidDifficulty() {
        assertThatThrownBy(() -> creator.createAllFromPool(ExamType.SQLD, 0))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> creator.createAllFromPool(ExamType.SQLD, 5))
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

        given(questionRepository.findMiniPoolBySubjectAndSource(any(), any(), any(), any()))
                .willReturn(List.of());

        assertThatThrownBy(() -> creator.createAllFromPool(ExamType.COMPUTER_LITERACY_2, null))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS);
    }

    @Test
    @DisplayName("createAllFromPool: 컴활 2급 풀(각 leaf+source 8문) → 회차 2개 생성, 원본 마킹 호출")
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

        // 미니 1회: 각 leaf 8문, source 분배 = 3,3,2 → 풀 각 8문이면 가능 회차 = min(8/3, 8/3, 8/2) = 2
        given(questionRepository.findMiniPoolBySubjectAndSource(eq(101L), eq(MockExamKind.PAST_EXAM), isNull(), isNull()))
                .willReturn(stubPool(8, 1000L));
        given(questionRepository.findMiniPoolBySubjectAndSource(eq(101L), eq(MockExamKind.AI), eq(MockExamVisibility.PUBLISHED), isNull()))
                .willReturn(stubPool(8, 2000L));
        given(questionRepository.findMiniPoolBySubjectAndSource(eq(101L), eq(MockExamKind.AI), eq(MockExamVisibility.PREMIUM), isNull()))
                .willReturn(stubPool(8, 3000L));
        given(questionRepository.findMiniPoolBySubjectAndSource(eq(102L), eq(MockExamKind.PAST_EXAM), isNull(), isNull()))
                .willReturn(stubPool(8, 4000L));
        given(questionRepository.findMiniPoolBySubjectAndSource(eq(102L), eq(MockExamKind.AI), eq(MockExamVisibility.PUBLISHED), isNull()))
                .willReturn(stubPool(8, 5000L));
        given(questionRepository.findMiniPoolBySubjectAndSource(eq(102L), eq(MockExamKind.AI), eq(MockExamVisibility.PREMIUM), isNull()))
                .willReturn(stubPool(8, 6000L));

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
                creator.createAllFromPool(ExamType.COMPUTER_LITERACY_2, null);

        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.createdMockExamIds()).hasSize(2);
        assertThat(result.examType()).isEqualTo(ExamType.COMPUTER_LITERACY_2);
        assertThat(result.appliedDifficulty()).isNull();

        // 잔여 풀: 각 leaf 당 PAST_EXAM 8-2*3=2, AI_PUB 8-2*3=2, AI_PREM 8-2*2=4
        // 합 (leaf 2개): PAST_EXAM=4, AI_PUB=4, AI_PREM=8
        assertThat(result.remainingBySource())
                .containsEntry(MiniMockExamCreator.Source.PAST_EXAM, 4L)
                .containsEntry(MiniMockExamCreator.Source.AI_PUBLISHED, 4L)
                .containsEntry(MiniMockExamCreator.Source.AI_PREMIUM, 8L);

        // 마킹 호출 검증 — 회차 2개 × 16문 = 32 원본 마킹
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass((Class) List.class);
        then(questionRepository).should().markIncludedInMiniInBatch(idsCaptor.capture(), any());
        assertThat(idsCaptor.getValue()).hasSize(32);

        // mockExam.save 도 2회 호출
        then(mockExamRepository).should(org.mockito.Mockito.times(2)).save(any(MockExamEntity.class));
    }

    @Test
    @DisplayName("createAllFromPool: 한 출처 풀만 모자라도 회차 수가 그 슬롯에 의해 제한됨")
    void createAllFromPool_bottleneckSourceLimitsRounds() {
        SubjectEntity root = newSubject(100L, null, "컴퓨터활용능력 2급 필기");
        SubjectEntity leaf1 = newSubject(101L, root, "컴퓨터 일반");
        SubjectEntity leaf2 = newSubject(102L, root, "스프레드시트 일반");

        given(subjectRepository.findByNameAndParentIsNull("컴퓨터활용능력 2급 필기"))
                .willReturn(Optional.of(root));
        given(subjectRepository.findByNameAndParentId("컴퓨터 일반", 100L))
                .willReturn(Optional.of(leaf1));
        given(subjectRepository.findByNameAndParentId("스프레드시트 일반", 100L))
                .willReturn(Optional.of(leaf2));

        // leaf1 의 AI_PUBLISHED 풀만 3문 → 미니 1회당 3문 필요 → 1회만 가능
        given(questionRepository.findMiniPoolBySubjectAndSource(eq(101L), eq(MockExamKind.PAST_EXAM), isNull(), isNull()))
                .willReturn(stubPool(20, 1000L));
        given(questionRepository.findMiniPoolBySubjectAndSource(eq(101L), eq(MockExamKind.AI), eq(MockExamVisibility.PUBLISHED), isNull()))
                .willReturn(stubPool(3, 2000L)); // 병목!
        given(questionRepository.findMiniPoolBySubjectAndSource(eq(101L), eq(MockExamKind.AI), eq(MockExamVisibility.PREMIUM), isNull()))
                .willReturn(stubPool(20, 3000L));
        given(questionRepository.findMiniPoolBySubjectAndSource(eq(102L), eq(MockExamKind.PAST_EXAM), isNull(), isNull()))
                .willReturn(stubPool(20, 4000L));
        given(questionRepository.findMiniPoolBySubjectAndSource(eq(102L), eq(MockExamKind.AI), eq(MockExamVisibility.PUBLISHED), isNull()))
                .willReturn(stubPool(20, 5000L));
        given(questionRepository.findMiniPoolBySubjectAndSource(eq(102L), eq(MockExamKind.AI), eq(MockExamVisibility.PREMIUM), isNull()))
                .willReturn(stubPool(20, 6000L));

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
                creator.createAllFromPool(ExamType.COMPUTER_LITERACY_2, null);

        assertThat(result.createdCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("createAllFromPool: difficulty=2 호출 시 풀 쿼리에 정확히 전파, 결과에도 표시됨")
    void createAllFromPool_difficultyPassthrough() {
        SubjectEntity root = newSubject(100L, null, "컴퓨터활용능력 2급 필기");
        SubjectEntity leaf1 = newSubject(101L, root, "컴퓨터 일반");
        SubjectEntity leaf2 = newSubject(102L, root, "스프레드시트 일반");

        given(subjectRepository.findByNameAndParentIsNull("컴퓨터활용능력 2급 필기"))
                .willReturn(Optional.of(root));
        given(subjectRepository.findByNameAndParentId("컴퓨터 일반", 100L))
                .willReturn(Optional.of(leaf1));
        given(subjectRepository.findByNameAndParentId("스프레드시트 일반", 100L))
                .willReturn(Optional.of(leaf2));

        // difficulty=2 인자가 정확히 전파되는지 검증 — 다른 difficulty 로는 풀이 없는 셈
        given(questionRepository.findMiniPoolBySubjectAndSource(any(), any(), any(), eq(2)))
                .willReturn(stubPool(8, 1000L));

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
                creator.createAllFromPool(ExamType.COMPUTER_LITERACY_2, 2);

        assertThat(result.appliedDifficulty()).isEqualTo(2);
        assertThat(result.createdCount()).isGreaterThanOrEqualTo(1);

        // findMiniPool 호출 시 difficulty=2 가 정확히 전달됐는지
        then(questionRepository).should(org.mockito.Mockito.atLeastOnce())
                .findMiniPoolBySubjectAndSource(any(), any(), any(), eq(2));
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
