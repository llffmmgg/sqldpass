package com.sqldpass.service.solve;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.solve.dto.SolveAnswerRequest;
import com.sqldpass.controller.solve.dto.SolveRequest;
import com.sqldpass.domain.solve.Solve;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class SolveServiceTest {

    @Autowired
    private SolveService solveService;

    @Autowired
    private SolveRepository solveRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    private MemberEntity member;
    private SubjectEntity subject;
    private QuestionEntity q1;
    private QuestionEntity q2;
    private QuestionEntity q3;

    @BeforeEach
    void setUp() {
        solveRepository.deleteAll();
        questionRepository.deleteAll();
        subjectRepository.deleteAll();
        memberRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        member = new MemberEntity("google", "123", "테스터");
        memberRepository.save(member);

        subject = new SubjectEntity(null, "SQL 기본", 1);
        subjectRepository.save(subject);

        q1 = new QuestionEntity(subject, "문제1", 1, "해설1");
        q2 = new QuestionEntity(subject, "문제2", 2, "해설2");
        q3 = new QuestionEntity(subject, "문제3", 3, "해설3");
        questionRepository.save(q1);
        questionRepository.save(q2);
        questionRepository.save(q3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("답안을 제출하면 채점 결과가 반환된다")
    void solve() {
        SolveRequest request = new SolveRequest(subject.getId(), null, List.of(
                new SolveAnswerRequest(q1.getId(), 1, null),  // 정답
                new SolveAnswerRequest(q2.getId(), 1, null),  // 오답 (정답: 2)
                new SolveAnswerRequest(q3.getId(), 3, null)   // 정답
        ));

        Solve result = solveService.solve(member.getId(), request).solve();

        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getCorrectCount()).isEqualTo(2);
        assertThat(result.getScore()).isEqualTo(67);
        assertThat(result.getAnswers()).hasSize(3);
        assertThat(result.getAnswers().get(0).isCorrect()).isTrue();
        assertThat(result.getAnswers().get(1).isCorrect()).isFalse();
    }

    @Test
    @DisplayName("내 풀이 기록을 조회하면 최신순으로 반환된다")
    void getMySolves() {
        SolveRequest request = new SolveRequest(subject.getId(), null, List.of(
                new SolveAnswerRequest(q1.getId(), 1, null)
        ));
        solveService.solve(member.getId(), request);
        entityManager.flush();
        entityManager.clear();

        List<Solve> solves = solveService.getMySolves(member.getId());

        assertThat(solves).hasSize(1);
        assertThat(solves.get(0).getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("mockExamId 가 null 이면 전체 시도, 값이 있으면 해당 시험 시도만 반환된다")
    void getMySolvesByMockExam() {
        // subject 풀이 1건 (mockExamId 없음)
        SolveRequest subjectReq = new SolveRequest(subject.getId(), null, List.of(
                new SolveAnswerRequest(q1.getId(), 1, null)
        ));
        solveService.solve(member.getId(), subjectReq);
        entityManager.flush();
        entityManager.clear();

        List<Solve> all = solveService.getMySolves(member.getId(), null);
        assertThat(all).hasSize(1);

        // 다른 mockExamId 로 필터하면 비어있어야 함
        List<Solve> filtered = solveService.getMySolves(member.getId(), 999L);
        assertThat(filtered).isEmpty();
    }
}
