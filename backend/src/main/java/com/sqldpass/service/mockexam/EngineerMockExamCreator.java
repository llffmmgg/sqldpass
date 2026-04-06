package com.sqldpass.service.mockexam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;

/**
 * 정보처리기사 실기 모의고사 생성기 — 20문항 세트.
 *
 * 실제 최근 회차 출제 경향을 반영한 4개 분포 템플릿 중 하나를 무작위 선택하여
 * 카테고리별로 샘플링한다. 같은 요청을 반복해도 매번 다른 분포가 나옴.
 *
 * 카테고리 (9):
 *   C언어 · Java · Python · SQL · 소프트웨어 설계 · 데이터베이스 이론 · 네트워크/OS · 보안 · 신기술 동향
 *
 * 시드 27개(카테고리당 3개)에선 일부 템플릿만 성공. 사용자가 JSON을 확장하면 모든 템플릿 사용 가능.
 */
@Component
@RequiredArgsConstructor
public class EngineerMockExamCreator {

    private static final String ROOT_SUBJECT_NAME = "정보처리기사 실기";

    // 카테고리 이름 상수 (오타 방지)
    private static final String C = "C언어";
    private static final String JAVA = "Java";
    private static final String PY = "Python";
    private static final String SQL = "SQL";
    private static final String DESIGN = "소프트웨어 설계";
    private static final String DB = "데이터베이스 이론";
    private static final String NETOS = "네트워크/OS";
    private static final String SEC = "보안";
    private static final String NEW = "신기술 동향";

    /** 템플릿 A — 프로그래밍 편중 */
    private static final Map<String, Integer> TEMPLATE_A = ordered(
            C, 2, JAVA, 3, PY, 2, SQL, 2,
            DESIGN, 3, DB, 2, NETOS, 2, SEC, 2, NEW, 2);

    /** 템플릿 B — 이론 편중 */
    private static final Map<String, Integer> TEMPLATE_B = ordered(
            C, 1, JAVA, 2, PY, 1, SQL, 2,
            DESIGN, 4, DB, 3, NETOS, 3, SEC, 2, NEW, 2);

    /** 템플릿 C — 균형형 (가장 최근 회차 유사) */
    private static final Map<String, Integer> TEMPLATE_C = ordered(
            C, 2, JAVA, 2, PY, 2, SQL, 3,
            DESIGN, 3, DB, 2, NETOS, 2, SEC, 2, NEW, 2);

    /** 템플릿 D — DB/SQL 강조 */
    private static final Map<String, Integer> TEMPLATE_D = ordered(
            C, 2, JAVA, 2, PY, 1, SQL, 4,
            DESIGN, 2, DB, 4, NETOS, 2, SEC, 2, NEW, 1);

    private static final List<Map<String, Integer>> TEMPLATES = List.of(
            TEMPLATE_A, TEMPLATE_B, TEMPLATE_C, TEMPLATE_D);

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final Random random = new Random();

    @Transactional
    public MockExamEntity create() {
        // 1) sequence + 이름
        int nextSeq = mockExamRepository.findMaxSequence().orElse(0) + 1;
        String name = "정보처리기사 실기 모의고사 " + nextSeq + "회";

        // 2) 템플릿 무작위 선택
        Map<String, Integer> distribution = TEMPLATES.get(random.nextInt(TEMPLATES.size()));

        // 3) 카테고리 이름 → subject id 매핑 (한 번만 조회)
        SubjectEntity root = subjectRepository.findByNameAndParentIsNull(ROOT_SUBJECT_NAME)
                .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND,
                        "'" + ROOT_SUBJECT_NAME + "' 루트 과목을 찾을 수 없습니다. V14 마이그레이션 미적용?"));

        Map<String, Long> categoryIds = new HashMap<>();
        for (String category : distribution.keySet()) {
            SubjectEntity leaf = subjectRepository.findByNameAndParentId(category, root.getId())
                    .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND,
                            "카테고리 '" + category + "'를 찾을 수 없습니다."));
            categoryIds.put(category, leaf.getId());
        }

        // 4) 카테고리별 샘플링 + 부족 검증
        List<QuestionEntity> picked = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            String category = entry.getKey();
            int needed = entry.getValue();
            Long subjectId = categoryIds.get(category);

            List<QuestionEntity> sub = questionRepository.findRandomBySubjectId(subjectId, needed);
            if (sub.size() < needed) {
                throw new SqldpassException(
                        ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        String.format("'%s' 카테고리 미편성 문항이 부족합니다. (필요 %d, 보유 %d) — JSON 시드를 더 추가해주세요.",
                                category, needed, sub.size()));
            }
            picked.addAll(sub);
        }

        // 5) 저장 + 배정
        MockExamEntity saved = mockExamRepository.save(new MockExamEntity(name, ExamType.ENGINEER_PRACTICAL, nextSeq));
        for (int i = 0; i < picked.size(); i++) {
            saved.linkQuestion(picked.get(i), i + 1);
        }
        return saved;
    }

    // === 헬퍼 ===
    private static Map<String, Integer> ordered(Object... kv) {
        LinkedHashMap<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return m;
    }
}
