package com.sqldpass.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.admin.AdminRevenueByPlan;
import com.sqldpass.controller.admin.AdminRevenueByProviderPlan;
import com.sqldpass.controller.admin.AdminRevenueByProviderPoint;
import com.sqldpass.controller.admin.AdminRevenuePoint;
import com.sqldpass.controller.admin.dto.AdminStatsResponse;
import com.sqldpass.controller.admin.dto.AdminStatsResponse.ActivityBucket;
import com.sqldpass.controller.admin.dto.AdminStatsResponse.CertActivity;
import com.sqldpass.controller.admin.dto.AdminStatsResponse.SubjectSolveStats;
import com.sqldpass.controller.admin.dto.AdminTrendResponse;
import com.sqldpass.controller.admin.dto.AdminTrendResponse.DailyPoint;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamKind;
import com.sqldpass.persistent.payment.PaymentRepository;
import com.sqldpass.persistent.solve.AnonymousSolveCountRepository;
import com.sqldpass.persistent.solve.SolveRepository;

@Service
@Transactional(readOnly = true)
public class AdminStatsService {

    private final AdminQuestionService adminQuestionService;
    private final MemberRepository memberRepository;
    private final SolveRepository solveRepository;
    private final AnonymousSolveCountRepository anonymousSolveCountRepository;
    private final PaymentRepository paymentRepository;

    public AdminStatsService(AdminQuestionService adminQuestionService,
                             MemberRepository memberRepository,
                             SolveRepository solveRepository,
                             AnonymousSolveCountRepository anonymousSolveCountRepository,
                             PaymentRepository paymentRepository) {
        this.adminQuestionService = adminQuestionService;
        this.memberRepository = memberRepository;
        this.solveRepository = solveRepository;
        this.anonymousSolveCountRepository = anonymousSolveCountRepository;
        this.paymentRepository = paymentRepository;
    }

    public AdminStatsResponse getStats() {
        List<SubjectSolveStats> subjectStats = solveRepository.findSubjectSolveStats().stream()
                .map(row -> new SubjectSolveStats(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()))
                .toList();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        long totalAnonymousSolves = anonymousSolveCountRepository.sumAll();
        Long todayAnonymousRaw = anonymousSolveCountRepository.countByDate(LocalDate.now());
        long todayAnonymousSolves = todayAnonymousRaw != null ? todayAnonymousRaw : 0L;

        List<CertActivity> certActivity = buildCertActivity(startOfToday);

        return new AdminStatsResponse(
                adminQuestionService.countAll(),
                adminQuestionService.countVerified(),
                adminQuestionService.countUnverified(),
                memberRepository.count(),
                solveRepository.count(),
                totalAnonymousSolves,
                adminQuestionService.countToday(),
                memberRepository.countByCreatedAtAfter(startOfToday),
                solveRepository.countByCreatedAtAfter(startOfToday),
                todayAnonymousSolves,
                subjectStats,
                certActivity);
    }

    /**
     * 자격증(ExamType) × 종류(MockExamKind=AI/PAST_EXAM) 별 풀이 활동을 빌드.
     * SolveRepository.findCertActivityBreakdown 가 native 단일 쿼리로 누적+오늘자 모두 반환.
     * 자격증 6종 고정 순서로 출력하며, 풀이가 없는 cert/kind 도 0 으로 폴백.
     */
    private List<CertActivity> buildCertActivity(LocalDateTime startOfToday) {
        List<Object[]> rows = solveRepository.findCertActivityBreakdown(startOfToday);

        Map<ExamType, Map<MockExamKind, ActivityBucket>> buckets = new java.util.EnumMap<>(ExamType.class);
        for (Object[] row : rows) {
            ExamType examType;
            MockExamKind kind;
            try {
                examType = ExamType.valueOf((String) row[0]);
                kind = MockExamKind.valueOf((String) row[1]);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ActivityBucket bucket = new ActivityBucket(
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue(),
                    ((Number) row[4]).longValue(),
                    ((Number) row[5]).longValue(),
                    ((Number) row[6]).longValue(),
                    ((Number) row[7]).longValue());
            buckets.computeIfAbsent(examType, k -> new java.util.EnumMap<>(MockExamKind.class))
                    .put(kind, bucket);
        }

        List<CertActivity> items = new java.util.ArrayList<>(6);
        items.add(buildCertActivityItem(ExamType.SQLD, "sqld", "SQLD", buckets));
        items.add(buildCertActivityItem(ExamType.ENGINEER_PRACTICAL, "engineer", "정보처리기사 실기", buckets));
        items.add(buildCertActivityItem(ExamType.ENGINEER_WRITTEN, "engineer-written", "정보처리기사 필기", buckets));
        items.add(buildCertActivityItem(ExamType.COMPUTER_LITERACY_1, "computer-literacy-1", "컴퓨터활용능력 1급 필기", buckets));
        items.add(buildCertActivityItem(ExamType.COMPUTER_LITERACY_2, "computer-literacy-2", "컴퓨터활용능력 2급 필기", buckets));
        items.add(buildCertActivityItem(ExamType.ADSP, "adsp", "데이터분석 준전문가(ADsP)", buckets));
        return items;
    }

    private CertActivity buildCertActivityItem(
            ExamType examType,
            String certSlug,
            String certName,
            Map<ExamType, Map<MockExamKind, ActivityBucket>> buckets) {
        Map<MockExamKind, ActivityBucket> kindMap = buckets.getOrDefault(examType, Map.of());
        return new CertActivity(
                certSlug,
                certName,
                kindMap.getOrDefault(MockExamKind.AI, ActivityBucket.empty()),
                kindMap.getOrDefault(MockExamKind.PAST_EXAM, ActivityBucket.empty()));
    }

    public AdminTrendResponse getTrend(int days) {
        int safe = Math.max(1, Math.min(days, 90));
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(safe - 1L);
        LocalDateTime since = start.atStartOfDay();

        Map<LocalDate, long[]> bucket = new HashMap<>();
        for (int i = 0; i < safe; i++) {
            bucket.put(start.plusDays(i), new long[] { 0L, 0L });
        }

        for (Object[] row : memberRepository.countByDaySince(since)) {
            LocalDate d = toLocalDate(row[0]);
            if (bucket.containsKey(d)) {
                bucket.get(d)[0] = ((Number) row[1]).longValue();
            }
        }
        for (Object[] row : solveRepository.countByDaySince(since)) {
            LocalDate d = toLocalDate(row[0]);
            if (bucket.containsKey(d)) {
                bucket.get(d)[1] = ((Number) row[1]).longValue();
            }
        }

        List<DailyPoint> points = bucket.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DailyPoint(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        return new AdminTrendResponse(safe, points);
    }

    private static LocalDate toLocalDate(Object raw) {
        if (raw instanceof java.sql.Date sql) {
            return sql.toLocalDate();
        }
        if (raw instanceof LocalDate ld) {
            return ld;
        }
        return LocalDate.parse(raw.toString());
    }

    /**
     * 일별 매출/환불/건수 추이 — 어드민 라인 차트용.
     * archived 구독 연결 결제는 제외(PaymentRepository.findDailyRevenue 가 처리).
     * 비어있는 날(결제 없음) 은 응답에서 빠짐 — 프론트가 누락 일자 0 으로 보간.
     */
    public List<AdminRevenuePoint> revenueTrend(int days) {
        int safe = Math.max(7, Math.min(days, 365));
        LocalDateTime since = LocalDate.now().minusDays(safe - 1L).atStartOfDay();
        return paymentRepository.findDailyRevenue(since).stream()
                .map(row -> new AdminRevenuePoint(
                        toLocalDate(row[0]),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).intValue()))
                .toList();
    }

    /** 플랜별 PAID 분포 — 어드민 막대 차트용. revenue DESC 정렬. archived 제외. */
    public List<AdminRevenueByPlan> revenueByPlan(int days) {
        int safe = Math.max(7, Math.min(days, 365));
        LocalDateTime since = LocalDate.now().minusDays(safe - 1L).atStartOfDay();
        return paymentRepository.findRevenueByPlan(since).stream()
                .map(row -> new AdminRevenueByPlan(
                        (String) row[0],
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).longValue()))
                .toList();
    }

    /**
     * 일자 × provider 분리 매출 추이 — 어드민 채널별 분리 라인 차트용.
     *
     * <p>{@link #revenueTrend(int)} 와 같은 days 가드(7~365 clamp) 적용. archived 구독 연결 결제 제외.
     * V79 이전 옛 결제는 provider 가 NULL 일 수 있어 "PORTONE" 으로 보정한다.
     */
    public List<AdminRevenueByProviderPoint> revenueByProvider(int days) {
        int safe = Math.max(7, Math.min(days, 365));
        LocalDateTime since = LocalDate.now().minusDays(safe - 1L).atStartOfDay();
        return paymentRepository.findDailyRevenueByProviderRaw(since).stream()
                .map(row -> new AdminRevenueByProviderPoint(
                        toLocalDate(row[0]),
                        row[1] == null ? "PORTONE" : (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).intValue()))
                .toList();
    }

    /**
     * provider × plan 분리 PAID 매출 분포 — 어드민 채널별 플랜 분포 차트용.
     *
     * <p>{@link #revenueByPlan(int)} 와 같은 가드(7~365 clamp). archived 제외 + plan NOT NULL.
     * NULL provider 는 "PORTONE" 으로 보정.
     */
    public List<AdminRevenueByProviderPlan> revenueByProviderAndPlan(int days) {
        int safe = Math.max(7, Math.min(days, 365));
        LocalDateTime since = LocalDate.now().minusDays(safe - 1L).atStartOfDay();
        return paymentRepository.findRevenueByProviderAndPlanRaw(since).stream()
                .map(row -> new AdminRevenueByProviderPlan(
                        row[0] == null ? "PORTONE" : (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).intValue(),
                        ((Number) row[3]).longValue()))
                .toList();
    }
}
