package com.sqldpass.service.question;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.domain.question.Question;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.SqldpassException;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class QuestionServiceTest {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private MockExamRepository mockExamRepository;

    @Autowired
    private EntityManager entityManager;

    private SubjectEntity subject;

    @BeforeEach
    void setUp() {
        questionRepository.deleteAll();
        mockExamRepository.deleteAll();
        subjectRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        subject = new SubjectEntity(null, "SQL 기본", 1);
        subjectRepository.save(subject);

        // 문제 풀 필터 요건: PUBLISHED + expertVerified 모의고사에 속해야 노출됨
        MockExamEntity mockExam = new MockExamEntity("테스트 회차", ExamType.SQLD, 1);
        mockExam.changeVisibility(MockExamVisibility.PUBLISHED);
        mockExam.toggleExpertVerified(); // false → true
        mockExamRepository.save(mockExam);

        QuestionEntity q1 = new QuestionEntity(subject, "문제1 내용", 1, "해설1");
        QuestionEntity q2 = new QuestionEntity(subject, "문제2 내용", 2, "해설2");
        QuestionEntity q3 = new QuestionEntity(subject, "문제3 내용", 3, "해설3");
        mockExam.linkQuestion(q1, 0);
        mockExam.linkQuestion(q2, 1);
        mockExam.linkQuestion(q3, 2);
        questionRepository.save(q1);
        questionRepository.save(q2);
        questionRepository.save(q3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("과목별 랜덤 문제를 조회하면 지정한 개수만큼 반환된다")
    void getRandomQuestions() {
        List<Question> questions = questionService.getRandomQuestions(subject.getId(), null, 2);

        assertThat(questions).hasSize(2);
        assertThat(questions.get(0).getSubjectId()).isEqualTo(subject.getId());
    }

    @Test
    @DisplayName("문제 상세를 조회하면 정답과 해설이 포함된다")
    void getQuestion() {
        Long questionId = questionRepository.findAll().get(0).getId();

        Question question = questionService.getQuestion(questionId);

        assertThat(question.getContent()).isNotBlank();
        assertThat(question.getCorrectOption()).isBetween(1, 4);
        assertThat(question.getExplanation()).isNotBlank();
    }

    @Test
    @DisplayName("존재하지 않는 문제를 조회하면 SqldpassException이 발생한다")
    void getQuestion_notFound() {
        assertThatThrownBy(() -> questionService.getQuestion(999L))
                .isInstanceOf(SqldpassException.class);
    }
}
