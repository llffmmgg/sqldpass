package com.sqldpass.service.publicapi;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.controller.publicapi.dto.PublicRankingResponse;
import com.sqldpass.controller.publicapi.dto.PublicStatsResponse;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.question.QuestionType;
import com.sqldpass.persistent.solve.SolveAnswerRepository;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PublicContentServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SolveAnswerRepository solveAnswerRepository;

    @Mock
    private SolveRepository solveRepository;

    @InjectMocks
    private PublicContentService publicContentService;

    @Test
    @DisplayName("getStats returns member and solve counts")
    void getStats() {
        given(memberRepository.count()).willReturn(100L);
        given(solveAnswerRepository.count()).willReturn(2500L);

        PublicStatsResponse response = publicContentService.getStats();

        assertThat(response.totalMembers()).isEqualTo(100L);
        assertThat(response.totalSolves()).isEqualTo(2500L);
    }

    @Test
    @DisplayName("getTopRanking maps ranking rows to ordered entries")
    void getTopRanking() {
        given(solveRepository.findTopRanking(any()))
                .willReturn(List.of(
                        new Object[]{"alpha", 15L},
                        new Object[]{"beta", 10L}
                ));

        PublicRankingResponse response = publicContentService.getTopRanking();

        assertThat(response.entries()).hasSize(2);
        assertThat(response.entries().get(0).rank()).isEqualTo(1);
        assertThat(response.entries().get(0).nickname()).isEqualTo("alpha");
        assertThat(response.entries().get(1).totalCorrect()).isEqualTo(10L);
        assertThat(response.generatedAt()).isNotNull();
    }

    @Test
    @DisplayName("listCategoriesByCert throws for an unknown certificate slug")
    void listCategoriesByCert_invalidSlug() {
        assertThatThrownBy(() -> publicContentService.listCategoriesByCert("unknown"))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SUBJECT_NOT_FOUND);
    }

    @Test
    @DisplayName("getQuestionDetail parses keywords and infers the SQLD certificate")
    void getQuestionDetail() {
        SubjectEntity subject = new SubjectEntity(null, "SQL Basics", 1);
        setSubjectId(subject, 7L);
        QuestionEntity question = new QuestionEntity(subject, "Question content", 2, "Explanation");
        question.update("Question content", 2, "Explanation", "Summary");
        setQuestionId(question, 10L);
        setQuestionField(question, "questionType", QuestionType.SHORT_ANSWER);
        setQuestionField(question, "answer", "answer text");
        setQuestionField(question, "keywords", "[\"join\",\"group by\"]");
        setQuestionField(question, "topic", "joins");
        setQuestionField(question, "difficulty", 2);

        given(questionRepository.findById(10L)).willReturn(Optional.of(question));

        var response = publicContentService.getQuestionDetail(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.certSlug()).isEqualTo("sqld");
        assertThat(response.categoryName()).isEqualTo("SQL Basics");
        assertThat(response.questionType()).isEqualTo("SHORT_ANSWER");
        assertThat(response.answer()).isEqualTo("answer text");
        assertThat(response.keywords()).containsExactly("join", "group by");
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

    private static void setQuestionId(QuestionEntity entity, Long id) {
        try {
            var field = QuestionEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setQuestionField(QuestionEntity entity, String name, Object value) {
        try {
            var field = QuestionEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
