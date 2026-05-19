package com.sqldpass.service.publicapi;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.config.CacheConfig;
import com.sqldpass.persistent.blog.BlogViewCountEntity;
import com.sqldpass.persistent.blog.BlogViewCountRepository;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicCategoryResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicCertResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicQuestionDetailResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicQuestionPageResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicQuestionSummary;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicSolveQuestionResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicSubjectResponse;
import com.sqldpass.controller.publicapi.dto.PublicRankingResponse;
import com.sqldpass.controller.publicapi.dto.PublicStatsResponse;
import com.sqldpass.controller.mockexam.dto.MockExamSummaryResponse;
import com.sqldpass.domain.question.Question;
import com.sqldpass.domain.subject.Subject;
import com.sqldpass.service.mockexam.MockExamService;
import com.sqldpass.service.question.QuestionService;
import com.sqldpass.service.subject.SubjectService;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.controller.publicapi.dto.PublicSolveQuotaResponse;
import com.sqldpass.persistent.solve.AnonymousSolveCountRepository;
import com.sqldpass.persistent.solve.AnonymousSolveIpQuotaRepository;
import com.sqldpass.persistent.solve.SolveAnswerRepository;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 공개(로그인 불필요) 콘텐츠 조회 서비스 — SEO 유입의 핵심.
 *
 * 자격증 구조:
 * - SQLD: parent=NULL인 루트 과목이 여러 개 ("1과목: ...", "2과목: ...") — 이들 밑의 leaf가 카테고리
 * - ENGINEER_PRACTICAL: parent=NULL이고 name="정보처리기사 실기"인 단일 루트 — 그 직접 자식이 카테고리
 *
 * 공개 URL 체계:
 * - /learn                         → listCerts()
 * - /learn/{certSlug}              → listCategoriesByCert()
 * - /learn/{certSlug}/cat-{id}     → listQuestionsByCategory()
 * - /q/{id}                        → getQuestionDetail()
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicContentService {

    public static final String SQLD_SLUG = "sqld";
    public static final String ENGINEER_SLUG = "engineer";
    public static final String COMPUTER_LITERACY_SLUG = "computer-literacy-1";
    public static final String ENGINEER_WRITTEN_SLUG = "engineer-written";
    public static final String COMPUTER_LITERACY_2_SLUG = "computer-literacy-2";
    public static final String ADSP_SLUG = "adsp";
    public static final String SQLD_NAME = "SQLD";
    public static final String ENGINEER_NAME = "정보처리기사 실기";
    public static final String COMPUTER_LITERACY_NAME = "컴퓨터활용능력 1급 필기";
    public static final String COMPUTER_LITERACY_2_NAME = "컴퓨터활용능력 2급 필기";
    public static final String ENGINEER_WRITTEN_NAME = "정보처리기사 필기";
    public static final String ADSP_NAME = "데이터분석 준전문가(ADsP)";
    private static final String ENGINEER_ROOT_SUBJECT_NAME = "정보처리기사 실기";
    private static final String COMPUTER_LITERACY_ROOT_SUBJECT_NAME = "컴퓨터활용능력 1급 필기";
    private static final String COMPUTER_LITERACY_2_ROOT_SUBJECT_NAME = "컴퓨터활용능력 2급 필기";
    private static final String ENGINEER_WRITTEN_ROOT_SUBJECT_NAME = "정보처리기사 필기";
    private static final String ADSP_ROOT_SUBJECT_NAME = "데이터분석 준전문가(ADsP)";
    private static final String SQLD_DESCRIPTION =
            "SQL 개발자(SQLD) 자격증 준비 — 1과목 데이터 모델링, 2과목 SQL 기본/활용 기출문제와 해설.";
    private static final String ENGINEER_DESCRIPTION =
            "정보처리기사 실기 준비 — C언어, Java, Python, SQL, 소프트웨어 설계, 데이터베이스, 네트워크/OS, 보안, 신기술 동향 기출 유형과 해설.";
    private static final String COMPUTER_LITERACY_DESCRIPTION =
            "컴퓨터활용능력 1급 필기 — 컴퓨터 일반, 스프레드시트 일반, 데이터베이스 일반 60문항 4지선다 기출 유형과 해설.";
    private static final String COMPUTER_LITERACY_2_DESCRIPTION =
            "컴퓨터활용능력 2급 필기 — 컴퓨터 일반, 스프레드시트 일반 40문항 4지선다 기출 유형과 해설.";
    private static final String ENGINEER_WRITTEN_DESCRIPTION =
            "정보처리기사 필기 준비 — 소프트웨어 설계, 소프트웨어 개발, 데이터베이스 구축, 프로그래밍 언어 활용, 정보시스템 구축 관리 100문항 4지선다 기출 유형과 해설.";
    private static final String ADSP_DESCRIPTION =
            "데이터분석 준전문가(ADsP) — 데이터 이해, 데이터 분석 기획, 데이터 분석 50문항 4지선다(2024 개편 반영) 기출 유형과 해설.";

    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final MemberRepository memberRepository;
    private final SolveAnswerRepository solveAnswerRepository;
    private final SolveRepository solveRepository;
    private final BlogViewCountRepository blogViewCountRepository;
    private final AnonymousSolveCountRepository anonymousSolveCountRepository;
    private final AnonymousSolveIpQuotaRepository anonymousSolveIpQuotaRepository;
    private final SubjectService subjectService;
    private final QuestionService questionService;
    private final MockExamService mockExamService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 비회원 1인(IP) 일일 풀이 한도. 자정에 자연 리셋. */
    public static final int ANON_DAILY_LIMIT = 10;

    // ----------------------------------------------------------
    // 랜딩 페이지 공개 캐시 (Caffeine, TTL 1시간 — CacheConfig에서 일괄 관리)
    // - ISR(1시간) + 백엔드 캐시(1시간) = 이중 안전망
    // - 봇/스캐너가 두드려도 DB 부담 사실상 0
    // ----------------------------------------------------------

    /**
     * 랜딩 페이지 노출용 공개 통계 — 회원 수 + 누적 풀이 수.
     */
    @Cacheable(CacheConfig.CACHE_PUBLIC_STATS)
    public PublicStatsResponse getStats() {
        long totalMembers = memberRepository.count();
        long totalSolves = solveAnswerRepository.count();
        return new PublicStatsResponse(totalMembers, totalSolves);
    }

    /**
     * 랜딩 페이지 노출용 TOP 30 랭킹 — 누적 정답 수 기준.
     * 1문제 이상 푼 사용자만 (INNER JOIN solve), 동점은 가입 순.
     */
    @Cacheable(CacheConfig.CACHE_PUBLIC_RANKING)
    public PublicRankingResponse getTopRanking() {
        List<Object[]> rows = solveRepository.findTopRanking(PageRequest.of(0, 30));
        List<PublicRankingResponse.Entry> entries = new ArrayList<>(rows.size());
        int rank = 1;
        for (Object[] row : rows) {
            String nickname = (String) row[0];
            long totalCorrect = ((Number) row[1]).longValue();
            entries.add(new PublicRankingResponse.Entry(rank++, nickname, totalCorrect));
        }
        return new PublicRankingResponse(entries, LocalDateTime.now());
    }

    // =================== 자격증 목록 ===================

    @Cacheable(CacheConfig.CACHE_PUBLIC_CERTS)
    public List<PublicCertResponse> listCerts() {
        List<PublicCategoryResponse> sqldCats = listSqldCategories();
        List<PublicCategoryResponse> engCats = listEngineerCategories();
        List<PublicCategoryResponse> cl1Cats = listComputerLiteracyCategories();
        List<PublicCategoryResponse> cl2Cats = listComputerLiteracy2Categories();
        List<PublicCategoryResponse> ewCats = listEngineerWrittenCategories();
        List<PublicCategoryResponse> adspCats = listAdspCategories();

        int sqldTotal = sqldCats.stream().mapToInt(PublicCategoryResponse::questionCount).sum();
        int engTotal = engCats.stream().mapToInt(PublicCategoryResponse::questionCount).sum();
        int cl1Total = cl1Cats.stream().mapToInt(PublicCategoryResponse::questionCount).sum();
        int cl2Total = cl2Cats.stream().mapToInt(PublicCategoryResponse::questionCount).sum();
        int ewTotal = ewCats.stream().mapToInt(PublicCategoryResponse::questionCount).sum();
        int adspTotal = adspCats.stream().mapToInt(PublicCategoryResponse::questionCount).sum();

        return List.of(
                new PublicCertResponse(SQLD_SLUG, SQLD_NAME, SQLD_DESCRIPTION, sqldTotal, sqldCats.size()),
                new PublicCertResponse(ENGINEER_SLUG, ENGINEER_NAME, ENGINEER_DESCRIPTION, engTotal, engCats.size()),
                new PublicCertResponse(COMPUTER_LITERACY_SLUG, COMPUTER_LITERACY_NAME, COMPUTER_LITERACY_DESCRIPTION, cl1Total, cl1Cats.size()),
                new PublicCertResponse(COMPUTER_LITERACY_2_SLUG, COMPUTER_LITERACY_2_NAME, COMPUTER_LITERACY_2_DESCRIPTION, cl2Total, cl2Cats.size()),
                new PublicCertResponse(ENGINEER_WRITTEN_SLUG, ENGINEER_WRITTEN_NAME, ENGINEER_WRITTEN_DESCRIPTION, ewTotal, ewCats.size()),
                new PublicCertResponse(ADSP_SLUG, ADSP_NAME, ADSP_DESCRIPTION, adspTotal, adspCats.size()));
    }

    // =================== 카테고리 목록 ===================

    @Cacheable(value = CacheConfig.CACHE_PUBLIC_CATEGORIES, key = "#certSlug")
    public List<PublicCategoryResponse> listCategoriesByCert(String certSlug) {
        return switch (certSlug) {
            case SQLD_SLUG -> listSqldCategories();
            case ENGINEER_SLUG -> listEngineerCategories();
            case COMPUTER_LITERACY_SLUG -> listComputerLiteracyCategories();
            case COMPUTER_LITERACY_2_SLUG -> listComputerLiteracy2Categories();
            case ENGINEER_WRITTEN_SLUG -> listEngineerWrittenCategories();
            case ADSP_SLUG -> listAdspCategories();
            default -> throw new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND, "알 수 없는 자격증: " + certSlug);
        };
    }

    private List<PublicCategoryResponse> listSqldCategories() {
        List<SubjectEntity> roots = subjectRepository.findRootsWithChildren();
        List<SubjectEntity> sqldRoots = new ArrayList<>();
        List<Long> childIds = new ArrayList<>();
        for (SubjectEntity root : roots) {
            if (ENGINEER_ROOT_SUBJECT_NAME.equals(root.getName())) continue;
            if (COMPUTER_LITERACY_ROOT_SUBJECT_NAME.equals(root.getName())) continue;
            if (COMPUTER_LITERACY_2_ROOT_SUBJECT_NAME.equals(root.getName())) continue;
            if (ENGINEER_WRITTEN_ROOT_SUBJECT_NAME.equals(root.getName())) continue;
            if (ADSP_ROOT_SUBJECT_NAME.equals(root.getName())) continue;
            sqldRoots.add(root);
            for (SubjectEntity child : sortByDisplayOrder(root.getChildren())) {
                childIds.add(child.getId());
            }
        }
        Map<Long, Integer> counts = countQuestionsBySubjectIds(childIds);
        List<PublicCategoryResponse> result = new ArrayList<>();
        for (SubjectEntity root : sqldRoots) {
            for (SubjectEntity child : sortByDisplayOrder(root.getChildren())) {
                int count = counts.getOrDefault(child.getId(), 0);
                result.add(new PublicCategoryResponse(child.getId(), child.getName(), root.getName(), count));
            }
        }
        return result;
    }

    private List<PublicCategoryResponse> listEngineerCategories() {
        return listSingleRootCategories(ENGINEER_ROOT_SUBJECT_NAME);
    }

    private List<PublicCategoryResponse> listComputerLiteracyCategories() {
        return listSingleRootCategories(COMPUTER_LITERACY_ROOT_SUBJECT_NAME);
    }

    private List<PublicCategoryResponse> listComputerLiteracy2Categories() {
        return listSingleRootCategories(COMPUTER_LITERACY_2_ROOT_SUBJECT_NAME);
    }

    private List<PublicCategoryResponse> listEngineerWrittenCategories() {
        return listSingleRootCategories(ENGINEER_WRITTEN_ROOT_SUBJECT_NAME);
    }

    private List<PublicCategoryResponse> listAdspCategories() {
        return listSingleRootCategories(ADSP_ROOT_SUBJECT_NAME);
    }

    private List<PublicCategoryResponse> listSingleRootCategories(String rootName) {
        SubjectEntity root = subjectRepository.findRootsWithChildren().stream()
                .filter(s -> rootName.equals(s.getName()))
                .findFirst()
                .orElse(null);
        if (root == null) return List.of();
        List<SubjectEntity> children = sortByDisplayOrder(root.getChildren());
        List<Long> childIds = children.stream().map(SubjectEntity::getId).toList();
        Map<Long, Integer> counts = countQuestionsBySubjectIds(childIds);
        List<PublicCategoryResponse> result = new ArrayList<>();
        for (SubjectEntity child : children) {
            int count = counts.getOrDefault(child.getId(), 0);
            result.add(new PublicCategoryResponse(child.getId(), child.getName(), root.getName(), count));
        }
        return result;
    }

    /** 카테고리 응답 정렬에 사용할 displayOrder 정렬. JOIN FETCH는 컬렉션 순서를 보장하지 않음. */
    private static List<SubjectEntity> sortByDisplayOrder(Collection<SubjectEntity> children) {
        List<SubjectEntity> sorted = new ArrayList<>(children);
        sorted.sort((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()));
        return sorted;
    }

    /** 여러 과목의 question 개수 일괄 집계 (1 GROUP BY 쿼리). */
    private Map<Long, Integer> countQuestionsBySubjectIds(Collection<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) return Collections.emptyMap();
        List<Object[]> rows = questionRepository.countBySubjectIdIn(new ArrayList<>(subjectIds));
        Map<Long, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }

    // =================== 카테고리별 문제 페이지네이션 ===================

    public PublicQuestionPageResponse listQuestionsByCategory(long categoryId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<QuestionEntity> pageData = questionRepository.findPublicBySubjectId(
                categoryId, PageRequest.of(Math.max(page, 0), safeSize));

        List<PublicQuestionSummary> summaries = pageData.getContent().stream()
                .map(q -> new PublicQuestionSummary(
                        q.getId(),
                        previewOf(q.getContent()),
                        q.getTopic(),
                        q.getDifficulty(),
                        q.getQuestionType() != null ? q.getQuestionType().name() : "MCQ"))
                .toList();

        return new PublicQuestionPageResponse(
                summaries,
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages());
    }

    // =================== 문제 상세 ===================

    public PublicQuestionDetailResponse getQuestionDetail(long id) {
        QuestionEntity q = questionRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.QUESTION_NOT_FOUND));

        SubjectEntity subject = q.getSubject();
        SubjectEntity parent = subject.getParent();
        String certSlug;
        String certName;
        if (parent != null && ENGINEER_ROOT_SUBJECT_NAME.equals(parent.getName())) {
            certSlug = ENGINEER_SLUG;
            certName = ENGINEER_NAME;
        } else if (parent != null && COMPUTER_LITERACY_ROOT_SUBJECT_NAME.equals(parent.getName())) {
            certSlug = COMPUTER_LITERACY_SLUG;
            certName = COMPUTER_LITERACY_NAME;
        } else if (parent != null && COMPUTER_LITERACY_2_ROOT_SUBJECT_NAME.equals(parent.getName())) {
            certSlug = COMPUTER_LITERACY_2_SLUG;
            certName = COMPUTER_LITERACY_2_NAME;
        } else if (parent != null && ENGINEER_WRITTEN_ROOT_SUBJECT_NAME.equals(parent.getName())) {
            certSlug = ENGINEER_WRITTEN_SLUG;
            certName = ENGINEER_WRITTEN_NAME;
        } else if (parent != null && ADSP_ROOT_SUBJECT_NAME.equals(parent.getName())) {
            certSlug = ADSP_SLUG;
            certName = ADSP_NAME;
        } else {
            certSlug = SQLD_SLUG;
            certName = SQLD_NAME;
        }

        List<String> keywords = parseKeywords(q.getKeywords());
        String questionType = q.getQuestionType() != null ? q.getQuestionType().name() : "MCQ";

        return new PublicQuestionDetailResponse(
                q.getId(),
                certSlug,
                certName,
                subject.getId(),
                subject.getName(),
                q.getContent(),
                questionType,
                q.getCorrectOption(),
                q.getAnswer(),
                keywords,
                q.getExplanation(),
                q.getTopic(),
                q.getDifficulty());
    }

    // =================== sitemap ===================

    public List<Long> listAllPublicQuestionIds() {
        return questionRepository.findAllPublicIds();
    }

    // =================== 오늘의 문제 (자격증별) ===================

    /**
     * 자격증별 오늘의 문제 1개 반환.
     * - 같은 날, 같은 자격증 → 모든 사용자에게 동일 문제 (공유성)
     * - 다른 자격증 → 서로 다른 문제 (seed에 cert 포함)
     */
    public PublicQuestionDetailResponse getDailyQuestion(String certSlug) {
        List<PublicCategoryResponse> categories = listCategoriesByCert(certSlug);
        if (categories.isEmpty()) {
            throw new SqldpassException(ErrorCode.QUESTION_NOT_FOUND, "자격증에 문제가 없습니다: " + certSlug);
        }
        List<Long> subjectIds = categories.stream().map(PublicCategoryResponse::id).toList();
        List<Long> questionIds = questionRepository.findIdsBySubjectIdIn(subjectIds);
        if (questionIds.isEmpty()) {
            throw new SqldpassException(ErrorCode.QUESTION_NOT_FOUND, "자격증에 문제가 없습니다: " + certSlug);
        }
        String seed = java.time.LocalDate.now() + ":" + certSlug;
        int idx = Math.floorMod(seed.hashCode(), questionIds.size());
        Long pickedId = questionIds.get(idx);
        return getQuestionDetail(pickedId);
    }

    // =================== 헬퍼 ===================

    private String previewOf(String content) {
        if (content == null) return "";
        String stripped = content.replaceAll("```[\\s\\S]*?```", "[코드]")
                .replaceAll("\\s+", " ")
                .trim();
        return stripped.length() > 120 ? stripped.substring(0, 120) + "..." : stripped;
    }

    @Transactional
    public void incrementBlogViewCount(String slug) {
        blogViewCountRepository.incrementViewCount(slug);
    }

    public Map<String, Long> getAllBlogViewCounts() {
        return blogViewCountRepository.findAll().stream()
                .collect(Collectors.toMap(BlogViewCountEntity::getSlug, BlogViewCountEntity::getViewCount));
    }

    private List<String> parseKeywords(String keywordsJson) {
        if (keywordsJson == null || keywordsJson.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(keywordsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 비로그인 /solve 화면용 Subject 트리 조회.
     */
    public List<PublicSubjectResponse> getSubjectTree() {
        return subjectService.getSubjectTree().stream()
                .map(this::toSubjectResponse)
                .toList();
    }

    private PublicSubjectResponse toSubjectResponse(Subject s) {
        return new PublicSubjectResponse(
                s.getId(),
                s.getName(),
                s.getDisplayOrder(),
                s.getChildren().stream().map(this::toSubjectResponse).toList()
        );
    }

    /**
     * 비로그인 /solve 화면용 랜덤 N개 문제. QuestionService 가 memberId=null 을 이미 지원.
     *
     * 비회원 일일 한도가 소진된 IP 면 진입 자체를 차단해 새 문제 페치를 막는다.
     * (현재 풀이 중인 문제는 이미 받아간 상태이므로 영향 없음)
     */
    public List<PublicSolveQuestionResponse> getRandomSolveQuestions(Long subjectId, int size, String clientIp) {
        if (clientIp != null) {
            int used = currentUsed(clientIp, java.time.LocalDate.now());
            if (used >= ANON_DAILY_LIMIT) {
                throw new SqldpassException(ErrorCode.ANONYMOUS_SOLVE_LIMIT_EXCEEDED);
            }
        }
        return questionService.getRandomQuestions(subjectId, null, size).stream()
                .map(q -> new PublicSolveQuestionResponse(
                        q.getId(),
                        q.getSubjectId(),
                        q.getContent(),
                        q.getQuestionType().name()
                ))
                .toList();
    }

    /**
     * 비회원 풀이 카운터 오늘자 +delta.
     *
     * - {@link AnonymousSolveCountEntity}(V41 일별 전체 합계)는 admin 통계용으로 그대로 +1
     * - {@link AnonymousSolveIpQuotaEntity}(V62 IP 단위 quota)도 +1 — 이쪽이 한도 차감의 진실
     *
     * 응답으로 한도 상태를 즉시 돌려줘 프런트가 별도 조회 없이 UI 갱신 가능.
     */
    @Transactional
    public PublicSolveQuotaResponse incrementAnonymousSolve(long delta, String clientIp) {
        java.time.LocalDate today = java.time.LocalDate.now();
        if (delta > 0) {
            anonymousSolveCountRepository.increment(today, delta);
            if (clientIp != null) {
                anonymousSolveIpQuotaRepository.increment(clientIp, today, (int) delta);
            }
        }
        int used = clientIp != null ? currentUsed(clientIp, today) : 0;
        return PublicSolveQuotaResponse.of(used, ANON_DAILY_LIMIT, today);
    }

    /**
     * 현재 IP 의 오늘 quota 조회 (증가 없음). 페이지 진입 시 헤더 칩 표시용.
     */
    public PublicSolveQuotaResponse getAnonymousSolveQuota(String clientIp) {
        java.time.LocalDate today = java.time.LocalDate.now();
        int used = clientIp != null ? currentUsed(clientIp, today) : 0;
        return PublicSolveQuotaResponse.of(used, ANON_DAILY_LIMIT, today);
    }

    private int currentUsed(String clientIp, java.time.LocalDate date) {
        Integer used = anonymousSolveIpQuotaRepository.usedCount(clientIp, date);
        return used != null ? used : 0;
    }

    /**
     * 비로그인 /mock-exams 화면용 모의고사 목록.
     * 로그인 사용자와 동일한 노출 조건(AI + 전문가 검수 + 비DRAFT)을 따른다.
     * best-score 정보는 비로그인이라 모두 null.
     */
    public List<MockExamSummaryResponse> listPublicMockExams() {
        return mockExamService.getAllForUser().stream()
                .map(MockExamSummaryResponse::from)
                .toList();
    }

    /**
     * 비로그인 /mini-mock-exams 화면용 미니 모의고사 목록.
     * MockExamKind=MINI 만, 정규와 동일한 visibility/expertVerified 정책.
     * best-score 정보는 비로그인이라 모두 null. PREMIUM 카드는 purchased=false 로 항상 잠금 — 카드 클릭 시
     * /mock-exams/{id} 의 PassPlusLockNotice 가 결제 유도.
     */
    public List<MockExamSummaryResponse> listPublicMiniMockExams() {
        return mockExamService.getAllMiniForUser().stream()
                .map(MockExamSummaryResponse::from)
                .toList();
    }
}
