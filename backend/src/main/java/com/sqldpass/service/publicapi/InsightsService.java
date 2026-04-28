package com.sqldpass.service.publicapi;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.config.CacheConfig;
import com.sqldpass.controller.publicapi.dto.InsightsDtos.HardestQuestionItem;
import com.sqldpass.controller.publicapi.dto.InsightsDtos.HardestQuestionsResponse;
import com.sqldpass.persistent.solve.HardestQuestionProjection;
import com.sqldpass.persistent.solve.SolveAnswerRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;

/**
 * 학습 인사이트 — 진지하게 푼 학생들이 자주 틀리는 문제를 가려낸다.
 *
 * 표본 필터 (전부 상수, 운영하면서 조정 가능):
 * - 학생 풀이수 ≥ {@link #MIN_MEMBER_ATTEMPTS}
 * - 학생 평균 정답률 ≥ {@link #MIN_MEMBER_ACCURACY} (운빨로 던진 사람 제외)
 * - 문제 시도수 ≥ {@link #MIN_QUESTION_ATTEMPTS} (저표본 noise 제거)
 * - 결과 갯수 = {@link #TOP_N}
 *
 * 캐시: Caffeine 1시간 (CacheConfig). solve_answer 100만 row 까지는 raw query
 * + 캐시 한 번이면 부담 거의 0.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsightsService {

    public static final int MIN_MEMBER_ATTEMPTS = 10;     // 적어도 10문제는 풀어봐야 통계 의미 있음
    public static final int MIN_MEMBER_ACCURACY = 50;     // 평균 정답률 50% 이상 (공부한 사람)
    public static final int MIN_QUESTION_ATTEMPTS = 5;    // 적어도 5번은 풀려야 (noise 제거)
    public static final int TOP_N = 30;                   // 오답률 best 30
    private static final int PREVIEW_LENGTH = 120;

    private final SolveAnswerRepository solveAnswerRepository;
    private final SubjectRepository subjectRepository;

    @Cacheable(value = CacheConfig.CACHE_HARDEST_QUESTIONS, key = "#subjectId")
    public HardestQuestionsResponse getHardestQuestions(long subjectId) {
        SubjectEntity subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.SUBJECT_NOT_FOUND));

        List<HardestQuestionProjection> rows = solveAnswerRepository.findHardestQuestions(
                subjectId,
                MIN_MEMBER_ATTEMPTS,
                MIN_MEMBER_ACCURACY,
                MIN_QUESTION_ATTEMPTS,
                TOP_N);

        List<HardestQuestionItem> items = rows.stream()
                .map(r -> new HardestQuestionItem(
                        r.getQuestionId(),
                        previewOf(r.getQuestionContent()),
                        r.getAttempts(),
                        r.getWrongCount(),
                        r.getWrongRate() != null ? r.getWrongRate() : 0.0))
                .toList();

        return new HardestQuestionsResponse(
                subject.getId(),
                subject.getName(),
                items.size(),
                items);
    }

    private static String previewOf(String content) {
        if (content == null) return "";
        String stripped = content
                .replaceAll("```[\\s\\S]*?```", "[코드]")
                .replaceAll("\\s+", " ")
                .trim();
        return stripped.length() > PREVIEW_LENGTH
                ? stripped.substring(0, PREVIEW_LENGTH) + "..."
                : stripped;
    }
}
