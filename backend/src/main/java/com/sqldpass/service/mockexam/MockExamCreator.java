package com.sqldpass.service.mockexam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;

/**
 * 모의고사 생성기 — 진짜 SQLD 시험 구성에 맞춘 50문항 세트 생성.
 *
 * 1과목: 데이터 모델링의 이해 (총 10문항)
 *   - 데이터 모델링의 이해 (id=3): 5문항
 *   - 데이터 모델과 SQL    (id=4): 5문항
 *
 * 2과목: SQL 기본 및 활용 (총 40문항)
 *   - SQL 기본 (id=5): 14문항
 *   - SQL 활용 (id=6): 13문항
 *   - 관리 구문 (id=7): 13문항
 *
 * 각 과목의 (아직 모의고사에 편성되지 않은) 문제 수가 부족하면
 * MOCK_EXAM_INSUFFICIENT_QUESTIONS 예외 반환.
 */
@Component
@RequiredArgsConstructor
public class MockExamCreator {

    /**
     * leaf subject id → 추출 문항 수.
     * LinkedHashMap으로 순서 고정 — 실제 SQLD 시험처럼 1과목(3,4) 먼저, 2과목(5,6,7) 뒤.
     */
    private static final Map<Long, Integer> DISTRIBUTION;
    static {
        LinkedHashMap<Long, Integer> m = new LinkedHashMap<>();
        m.put(3L, 5);   // 1과목: 데이터 모델링의 이해
        m.put(4L, 5);   // 1과목: 데이터 모델과 SQL
        m.put(5L, 14);  // 2과목: SQL 기본
        m.put(6L, 13);  // 2과목: SQL 활용
        m.put(7L, 13);  // 2과목: 관리 구문
        DISTRIBUTION = m;
    }

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public MockExamEntity create() {
        // 1) SQLD 자격증 내 다음 sequence 계산
        int nextSeq = mockExamRepository.findMaxSequenceByExamType(ExamType.SQLD).orElse(0) + 1;
        String name = "SQLD 모의고사 " + nextSeq + "회";

        // 2) 각 과목별로 미편성 문제에서 랜덤 추출 + 부족 검증
        List<QuestionEntity> picked = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : DISTRIBUTION.entrySet()) {
            Long subjectId = entry.getKey();
            int needed = entry.getValue();

            List<QuestionEntity> sub = questionRepository.findRandomBySubjectId(subjectId, needed);
            if (sub.size() < needed) {
                throw new SqldpassException(
                        ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        String.format("과목 ID %d 미편성 문항이 부족합니다. (필요 %d, 보유 %d)",
                                subjectId, needed, sub.size()));
            }
            picked.addAll(sub);
        }

        // 3) MockExam 저장 후 문제 배정 (mockExam.id 필요)
        MockExamEntity saved = mockExamRepository.save(new MockExamEntity(name, nextSeq));
        for (int i = 0; i < picked.size(); i++) {
            saved.linkQuestion(picked.get(i), i + 1);
        }
        return saved;
    }
}
