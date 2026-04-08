package com.sqldpass.service.mockexam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;
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
import com.sqldpass.service.notification.DiscordNotifier;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * 정보처리기사 실기 모의고사 즉석 생성기 — 20문항 세트.
 *
 * 흐름 (시드 풀 다중화 + 중복 검증 버전):
 *   1) 4개 분포 템플릿 중 1개 무작위 선택
 *   2) 각 카테고리마다 EngineerTopicExamples 시드 풀에서 needed개 시드를 무작위 추출
 *      → 시드별로 1개씩 변형 생성을 AI에 요청 (자연스러운 난이도 분산)
 *   3) 직전 N개 정답/본문에서 forbidden identifiers + recentAnswers 추출 → 프롬프트에 주입
 *   4) AI 응답 검증: 시드 식별자/최근 정답과의 충돌 감지 → 1회 재시도
 *   5) 검증 통과한 문제를 QuestionEntity로 저장
 *   6) 20문제를 MockExamEntity에 묶어 displayOrder 부여
 */
@Slf4j
@Component
public class EngineerMockExamCreator {

    private static final String ROOT_SUBJECT_NAME = "정보처리기사 실기";
    private static final int RECENT_LOOKBACK = 30; // 직전 30개에서 forbidden 시그널 추출
    private static final int MAX_REGENERATION = 1; // 중복 발견 시 재시도 횟수

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
    private final DiscordNotifier discordNotifier;
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EngineerMockExamCreator(MockExamRepository mockExamRepository,
                                   QuestionRepository questionRepository,
                                   SubjectRepository subjectRepository,
                                   @Qualifier("generator") AiProvider engineerAiProvider,
                                   DiscordNotifier discordNotifier) {
        this.mockExamRepository = mockExamRepository;
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.engineerAiProvider = engineerAiProvider;
        this.discordNotifier = discordNotifier;
    }

    @Transactional
    public MockExamEntity create() {
        return create(MockExamDifficulty.NORMAL);
    }

    @Transactional
    public MockExamEntity create(MockExamDifficulty mockExamDifficulty) {
        MockExamDifficulty difficulty = mockExamDifficulty != null ? mockExamDifficulty : MockExamDifficulty.NORMAL;
        int nextSeq = mockExamRepository.findMaxSequenceByExamType(ExamType.ENGINEER_PRACTICAL).orElse(0) + 1;
        String name = "정보처리기사 실기 모의고사 " + nextSeq + "회 (" + difficulty.label() + ")";

        Map<String, Integer> distribution = TEMPLATES.get(random.nextInt(TEMPLATES.size()));
        int totalQuestions = distribution.values().stream().mapToInt(Integer::intValue).sum();

        // 사용자 지정 평균 난이도에 따른 분포 슬롯 (셔플된 [1,1,2,2,2,3,3,...])
        List<Integer> difficultySlots = buildDifficultySlots(difficulty, totalQuestions);

        log.info("정처기 모의고사 생성 시작 - sequence={}, 분포={}, 평균난이도={}, 슬롯={}",
                nextSeq, distribution, difficulty, difficultySlots);

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

        // 회차 내부 정답 수집 (회차 내 중복도 함께 차단)
        Set<String> exhibitedAnswersInThisExam = new HashSet<>();

        List<QuestionEntity> picked = new ArrayList<>();
        int slotCursor = 0; // difficultySlots 진행 인덱스
        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            String category = entry.getKey();
            int needed = entry.getValue();
            SubjectEntity subject = categorySubjects.get(category);

            // 1) 시드 풀에서 needed개 시드 무작위 추출
            List<EngineerExample> seeds = EngineerTopicExamples.randomFor(category, needed, random);
            if (seeds.isEmpty() || seeds.size() < needed) {
                throw new SqldpassException(ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                        "카테고리 '" + category + "' 시드 풀이 부족합니다 (필요 " + needed + ", 보유 " + seeds.size() + ")");
            }

            // 1-1) 이 카테고리에 할당된 목표 난이도 슬롯 needed개 슬라이스
            List<Integer> targetDifficulties = new ArrayList<>(
                    difficultySlots.subList(slotCursor, slotCursor + needed));
            slotCursor += needed;

            // 2) 회피 신호 수집: 시드 식별자 + 최근 출제 식별자 + 최근 정답
            Pageable lookback = PageRequest.of(0, RECENT_LOOKBACK);
            List<String> recentAnswers = questionRepository
                    .findRecentAnswersBySubjectId(subject.getId(), lookback);
            List<String> recentContents = questionRepository
                    .findRecentContentsBySubjectId(subject.getId(), lookback);
            List<String> existingSummaries = questionRepository.findSummariesBySubjectId(subject.getId());

            List<String> forbiddenIdentifiers = collectForbiddenIdentifiers(seeds, recentContents);

            // 3) AI 호출 (1회차) — 목표 난이도 전달
            AiGenerationRequest request = new AiGenerationRequest(
                    category, subject.getId(), seeds.get(0).topic(),
                    existingSummaries, needed, ExamType.ENGINEER_PRACTICAL);
            List<GeneratedQuestion> generated = callAi(request, seeds, targetDifficulties, forbiddenIdentifiers, recentAnswers, needed);

            // 4) 중복 검증: 정답이 최근/회차내와 8자+ 일치 → 재시도
            List<GeneratedQuestion> validated = validateAndRetry(
                    request, seeds, targetDifficulties, forbiddenIdentifiers, recentAnswers,
                    exhibitedAnswersInThisExam, generated, needed, category);

            // 5) 저장 + 회차내 정답 풀에 등록 — 사용자 지정 난이도로 저장
            for (int i = 0; i < needed; i++) {
                GeneratedQuestion gq = validated.get(i);
                EngineerExample seed = seeds.get(i);
                int targetDifficulty = targetDifficulties.get(i);
                QuestionEntity entity = toEngineerEntity(subject, gq, seed, targetDifficulty);
                picked.add(questionRepository.save(entity));
                if (gq.answerText() != null) {
                    exhibitedAnswersInThisExam.add(normalizeAnswer(gq.answerText()));
                }
            }
        }

        MockExamEntity saved = mockExamRepository.save(
                new MockExamEntity(name, ExamType.ENGINEER_PRACTICAL, nextSeq));
        for (int i = 0; i < picked.size(); i++) {
            saved.linkQuestion(picked.get(i), i + 1);
        }
        log.info("정처기 모의고사 생성 완료 - id={}, 문항수={}", saved.getId(), picked.size());

        // 디스코드 알림 (generation 채널 재사용) — 실패해도 비즈니스 로직에 영향 없음
        Map<String, Long> categoryDist = picked.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getSubject().getName(),
                        TreeMap::new,
                        Collectors.counting()));
        discordNotifier.notifyEngineerMockExamGenerated(saved, picked.size(), categoryDist);

        return saved;
    }

    /**
     * 사용자 지정 평균 난이도(EASY/NORMAL/HARD)에 따라
     * 전체 문항 수만큼의 난이도 슬롯 리스트를 만들고 셔플하여 반환.
     *
     * 분포 (20문제 기준):
     * - EASY:   60/30/10  → 12 / 6 / 2
     * - NORMAL: 25/50/25  → 5  / 10 / 5
     * - HARD:   10/30/60  → 2  / 6  / 12
     */
    private List<Integer> buildDifficultySlots(MockExamDifficulty difficulty, int totalQuestions) {
        int[] dist = switch (difficulty) {
            case EASY -> new int[]{60, 30, 10};
            case NORMAL -> new int[]{25, 50, 25};
            case HARD -> new int[]{10, 30, 60};
        };
        int l1 = Math.round(totalQuestions * dist[0] / 100f);
        int l2 = Math.round(totalQuestions * dist[1] / 100f);
        int l3 = totalQuestions - l1 - l2; // 반올림 오차 보정
        if (l3 < 0) {
            // 극단적 보정: l2에서 깎음
            l2 += l3;
            l3 = 0;
        }
        List<Integer> slots = new ArrayList<>(totalQuestions);
        for (int i = 0; i < l1; i++) slots.add(1);
        for (int i = 0; i < l2; i++) slots.add(2);
        for (int i = 0; i < l3; i++) slots.add(3);
        Collections.shuffle(slots, random);
        return slots;
    }

    /** AI 호출 + 응답 길이 검증 */
    private List<GeneratedQuestion> callAi(AiGenerationRequest request,
                                           List<EngineerExample> seeds,
                                           List<Integer> targetDifficulties,
                                           List<String> forbiddenIdentifiers,
                                           List<String> recentAnswers,
                                           int needed) {
        AiGenerationResponse response = engineerAiProvider
                .generateEngineerQuestions(request, seeds, targetDifficulties, forbiddenIdentifiers, recentAnswers);
        List<GeneratedQuestion> generated = response.questions();
        if (generated == null || generated.size() < needed) {
            throw new SqldpassException(
                    ErrorCode.MOCK_EXAM_INSUFFICIENT_QUESTIONS,
                    String.format("'%s' AI 생성 실패 (필요 %d, 생성 %d) — AI 응답을 확인하세요.",
                            request.subjectName(), needed, generated == null ? 0 : generated.size()));
        }
        return generated;
    }

    /**
     * 생성 결과 중 정답이 (최근 출제 정답 ∪ 회차 내 이미 등록된 정답)과 8자+ 일치하는 문제가 있으면
     * 강화된 회피 신호로 1회 재시도. 그래도 충돌이 남으면 그대로 통과(무한 루프 방지).
     */
    private List<GeneratedQuestion> validateAndRetry(AiGenerationRequest request,
                                                     List<EngineerExample> seeds,
                                                     List<Integer> targetDifficulties,
                                                     List<String> forbiddenIdentifiers,
                                                     List<String> recentAnswers,
                                                     Set<String> exhibitedInExam,
                                                     List<GeneratedQuestion> initial,
                                                     int needed,
                                                     String category) {
        List<GeneratedQuestion> current = initial;
        for (int attempt = 0; attempt <= MAX_REGENERATION; attempt++) {
            List<String> conflicts = findAnswerConflicts(current, recentAnswers, exhibitedInExam, needed);
            if (conflicts.isEmpty()) {
                return current;
            }
            log.warn("정처기 카테고리 '{}' 정답 충돌 감지 (attempt={}): {}", category, attempt, conflicts);

            if (attempt == MAX_REGENERATION) {
                log.warn("정처기 카테고리 '{}' 재시도 한계 도달 - 충돌 잔존한 채로 통과", category);
                return current;
            }

            // 강화된 forbidden 목록으로 재시도
            List<String> strengthenedForbidden = new ArrayList<>(forbiddenIdentifiers);
            List<String> strengthenedAnswers = new ArrayList<>(recentAnswers);
            strengthenedAnswers.addAll(conflicts);
            current = callAi(request, seeds, targetDifficulties, strengthenedForbidden, strengthenedAnswers, needed);
        }
        return current;
    }

    /** 정답이 최근 출제 정답 또는 회차 내 정답과 8자+ 일치하는지 검사 → 충돌 정답 리스트 반환 */
    private List<String> findAnswerConflicts(List<GeneratedQuestion> generated,
                                             List<String> recentAnswers,
                                             Set<String> exhibitedInExam,
                                             int needed) {
        List<String> conflicts = new ArrayList<>();
        Set<String> recentNormalized = new HashSet<>();
        for (String r : recentAnswers) {
            if (r != null && !r.isBlank()) {
                recentNormalized.add(normalizeAnswer(r));
            }
        }
        for (int i = 0; i < Math.min(needed, generated.size()); i++) {
            String ans = generated.get(i).answerText();
            if (ans == null || ans.isBlank()) continue;
            String norm = normalizeAnswer(ans);
            if (norm.length() < 8) continue; // 짧은 답(예: "2")은 회피 대상에서 제외
            if (exhibitedInExam.contains(norm)) {
                conflicts.add(ans);
                continue;
            }
            for (String existing : recentNormalized) {
                if (existing.length() >= 8 && (existing.equals(norm) || norm.contains(existing) || existing.contains(norm))) {
                    conflicts.add(ans);
                    break;
                }
            }
        }
        return conflicts;
    }

    /** 정답 비교를 위한 정규화: 공백 압축 + 소문자 */
    private String normalizeAnswer(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    /** 시드 식별자 + 최근 출제 본문에서 추출한 식별자 합집합 (최대 25개) */
    private List<String> collectForbiddenIdentifiers(List<EngineerExample> seeds, List<String> recentContents) {
        Set<String> all = new LinkedHashSet<>(EngineerTopicExamples.identifiersOfAll(seeds));
        for (String content : recentContents) {
            all.addAll(extractIdentifiersFromContent(content));
            if (all.size() >= 25) break;
        }
        List<String> result = new ArrayList<>(all);
        return result.subList(0, Math.min(result.size(), 25));
    }

    private static final Pattern CODE_FENCE = Pattern.compile("```[a-zA-Z]*\\s*([\\s\\S]*?)```");
    private static final Pattern IDENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{2,}");
    private static final Set<String> RESERVED = new HashSet<>(Arrays.asList(
            "int", "void", "char", "long", "short", "double", "float", "bool", "true", "false", "null",
            "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "return", "default",
            "class", "interface", "abstract", "extends", "implements", "static", "final", "public", "private",
            "protected", "new", "this", "super", "import", "package", "throws", "throw", "try", "catch", "finally",
            "def", "lambda", "yield", "elif", "and", "or", "not", "self",
            "print", "println", "printf", "input", "len", "range", "list", "dict", "tuple", "set", "str",
            "include", "stdio", "main", "sizeof", "struct", "typedef", "enum", "union", "const", "extern",
            "select", "from", "where", "group", "order", "having", "join", "inner", "outer", "left", "right",
            "union", "all", "distinct", "asc", "desc", "limit", "offset", "into", "values", "table", "view",
            "primary", "foreign", "key", "index", "create", "drop", "alter", "update", "delete", "insert",
            "System", "out", "String", "Integer", "Double", "Float", "Object", "Boolean", "Math",
            "Arrays", "Collections", "ArrayList", "HashMap", "HashSet", "List", "Map", "Set", "Stream",
            "args", "stdin", "stdout", "stderr", "size", "length", "value", "result", "data", "item", "items"));

    private List<String> extractIdentifiersFromContent(String content) {
        if (content == null || content.isBlank()) return List.of();
        List<String> found = new ArrayList<>();
        Matcher fenceMatcher = CODE_FENCE.matcher(content);
        while (fenceMatcher.find()) {
            String code = fenceMatcher.group(1);
            Matcher idMatcher = IDENT.matcher(code);
            while (idMatcher.find()) {
                String ident = idMatcher.group();
                if (!RESERVED.contains(ident) && !RESERVED.contains(ident.toLowerCase())) {
                    found.add(ident);
                }
            }
        }
        return found;
    }

    /** AI 생성 응답을 정처기 단답/약술형 QuestionEntity로 변환 (사용자 지정 난이도 사용) */
    private QuestionEntity toEngineerEntity(SubjectEntity subject, GeneratedQuestion gq, EngineerExample seed, int targetDifficulty) {
        QuestionType qt;
        try {
            qt = gq.questionType() != null ? QuestionType.valueOf(gq.questionType()) : seed.questionType();
        } catch (IllegalArgumentException e) {
            qt = seed.questionType();
        }
        if (qt == QuestionType.MCQ) {
            qt = seed.questionType();
        }

        String answer = gq.answerText() != null ? gq.answerText() : "";
        String keywordsJson;
        try {
            List<String> kws = gq.keywords() != null ? gq.keywords() : List.of();
            keywordsJson = objectMapper.writeValueAsString(kws);
        } catch (Exception e) {
            keywordsJson = "[]";
        }

        // 난이도: 사용자가 지정한 targetDifficulty를 강제 사용 (AI/시드 값 무시)
        return new QuestionEntity(
                subject, gq.content(), qt,
                answer, keywordsJson, gq.explanation(),
                gq.summary(), seed.topic(),
                targetDifficulty);
    }

    private static Map<String, Integer> ordered(Object... kv) {
        LinkedHashMap<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return m;
    }
}
