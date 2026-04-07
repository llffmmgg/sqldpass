package com.sqldpass.service.mockexam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.question.QuestionType;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.generation.AiProvider;
import com.sqldpass.service.generation.EngineerTopicExamples;
import com.sqldpass.service.generation.EngineerTopicExamples.EngineerExample;
import com.sqldpass.service.generation.dto.AiGenerationRequest;
import com.sqldpass.service.generation.dto.AiGenerationResponse;
import com.sqldpass.service.generation.dto.GeneratedQuestion;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * 정보처리기사 실기 모의고사 즉석 생성기 — 20문항 세트.
 *
 * 흐름:
 *   1) 4개 분포 템플릿 중 1개 무작위 선택 (rotation)
 *   2) 각 카테고리마다 EngineerTopicExamples의 고난도 예시를 few-shot으로 AI에 전달
 *   3) AI가 변형 N개 생성 → QuestionEntity로 저장
 *   4) 20문제를 MockExamEntity에 묶어 displayOrder 부여
 *
 * 풀에 사전 채울 필요 없음. 누를 때마다 전혀 새로운 모의고사가 생성됨.
 * existingSummaries 회피 메커니즘으로 호출이 누적될수록 다양성이 확보됨.
 */
@Slf4j
@Component
public class EngineerMockExamCreator {

    private static final String ROOT_SUBJECT_NAME = "정보처리기사 실기";

    // 카테고리 이름 상수 (EngineerTopicExamples 의 키와 일치)
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
    private final AiProvider engineerAiProvider;
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EngineerMockExamCreator(MockExamRepository mockExamRepository,
                                   QuestionRepository questionRepository,
                                   SubjectRepository subjectRepository,
                                   @Qualifier("generator") AiProvider engineerAiProvider) {
        this.mockExamRepository = mockExamRepository;
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.engineerAiProvider = engineerAiProvider;
    }

    @Transactional
    public MockExamEntity create() {
        // 1) sequence + 이름
        int nextSeq = mockExamRepository.findMaxSequence().orElse(0) + 1;
        String name = "정보처리기사 실기 모의고사 " + nextSeq + "회";

        // 2) 템플릿 무작위 선택
        Map<String, Integer> distribution = TEMPLATES.get(random.nextInt(TEMPLATES.size()));
        log.info("정처기 모의고사 생성 시작 - sequence={}, 분포={}", nextSeq, distribution);

        // 3) 카테고리 → subject id 매핑 (한 번만 조회)
        SubjectEntity root = subjectRepository.findByNameAndParentIsNull(ROOT_SUBJECT_NAME)
                .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND,
                        "'" + ROOT_SUBJECT_NAME + "' 루트 과목을 찾을 수 없습니다. V14 마이그레이션 미적용?"));

        Map<String, SubjectEntity> categorySubjects = new HashMap<>();
        for (String category : distribution.keySet()) {
            SubjectEntity leaf = subjectRepository.findByNameAndParentId(category, root.getId())
                    .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND,
                            "카테고리 '" + category + "'를 찾을 수 없습니다."));
            categorySubjects.put(category, leaf);
        }

        // 4) 카테고리별 AI 생성 + 저장
        List<QuestionEntity> picked = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            String category = entry.getKey();
            int needed = entry.getValue();
            SubjectEntity subject = categorySubjects.get(category);

            EngineerExample example = EngineerTopicExamples.get(category);
            if (example == null) {
                throw new SqldpassException(ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        "EngineerTopicExamples에 카테고리 '" + category + "' 예시가 없습니다.");
            }

            List<String> existingSummaries = questionRepository.findSummariesBySubjectId(subject.getId());

            AiGenerationRequest request = new AiGenerationRequest(
                    category, subject.getId(), example.topic(),
                    existingSummaries, needed, ExamType.ENGINEER_PRACTICAL);

            AiGenerationResponse response = engineerAiProvider.generateEngineerQuestions(request, example);
            List<GeneratedQuestion> generated = response.questions();

            if (generated == null || generated.size() < needed) {
                throw new SqldpassException(
                        ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        String.format("'%s' AI 생성 실패 (필요 %d, 생성 %d) — AI 응답을 확인하세요.",
                                category, needed, generated == null ? 0 : generated.size()));
            }

            // 응답 N개 → QuestionEntity 변환 + 저장
            for (int i = 0; i < needed; i++) {
                GeneratedQuestion gq = generated.get(i);
                QuestionEntity entity = toEngineerEntity(subject, gq, example);
                picked.add(questionRepository.save(entity));
            }
        }

        // 5) MockExamEntity 저장 + 20문제 순서 부여
        MockExamEntity saved = mockExamRepository.save(
                new MockExamEntity(name, ExamType.ENGINEER_PRACTICAL, nextSeq));
        for (int i = 0; i < picked.size(); i++) {
            saved.linkQuestion(picked.get(i), i + 1);
        }
        log.info("정처기 모의고사 생성 완료 - id={}, 문항수={}", saved.getId(), picked.size());
        return saved;
    }

    /** AI 생성 응답을 정처기 단답/약술형 QuestionEntity로 변환 */
    private QuestionEntity toEngineerEntity(SubjectEntity subject, GeneratedQuestion gq, EngineerExample example) {
        // questionType: 응답 우선, 없으면 예시의 유형 그대로
        QuestionType qt;
        try {
            qt = gq.questionType() != null ? QuestionType.valueOf(gq.questionType()) : example.questionType();
        } catch (IllegalArgumentException e) {
            qt = example.questionType();
        }
        // MCQ가 잘못 들어온 경우 예시 유형으로 강제 교체
        if (qt == QuestionType.MCQ) {
            qt = example.questionType();
        }

        String answer = gq.answerText() != null ? gq.answerText() : "";
        String keywordsJson;
        try {
            List<String> kws = gq.keywords() != null ? gq.keywords() : List.of();
            keywordsJson = objectMapper.writeValueAsString(kws);
        } catch (Exception e) {
            keywordsJson = "[]";
        }

        return new QuestionEntity(
                subject, gq.content(), qt,
                answer, keywordsJson, gq.explanation(),
                gq.summary(), example.topic(),
                gq.difficulty() != null ? gq.difficulty() : 5);
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
