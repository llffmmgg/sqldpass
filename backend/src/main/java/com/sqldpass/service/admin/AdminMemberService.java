package com.sqldpass.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.admin.dto.AdminMemberDashboardResponse;
import com.sqldpass.controller.admin.dto.AdminMemberDashboardResponse.DailyActivity;
import com.sqldpass.controller.admin.dto.AdminMemberDashboardResponse.MemberInfo;
import com.sqldpass.controller.admin.dto.AdminMemberDashboardResponse.RecentSolve;
import com.sqldpass.controller.admin.dto.AdminMemberDashboardResponse.Stats;
import com.sqldpass.controller.admin.dto.AdminMemberDashboardResponse.SubjectStat;
import com.sqldpass.controller.admin.dto.AdminMemberDashboardResponse.WeakSubject;
import com.sqldpass.controller.admin.dto.AdminMemberResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerStatsResponse;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.solve.SolveEntity;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.wronganswer.WrongAnswerService;

@Service
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;
    private final SolveRepository solveRepository;
    private final WrongAnswerService wrongAnswerService;

    public AdminMemberService(MemberRepository memberRepository,
                              SolveRepository solveRepository,
                              WrongAnswerService wrongAnswerService) {
        this.memberRepository = memberRepository;
        this.solveRepository = solveRepository;
        this.wrongAnswerService = wrongAnswerService;
    }

    public Page<AdminMemberResponse> getMembers(int page, int size) {
        // 랭킹용 더미 시드(provider='SEED', V20 마이그레이션)는 어드민 목록에서 숨김
        Page<MemberEntity> memberPage = memberRepository.findByProviderNot(
                "SEED",
                PageRequest.of(page, size, Sort.by("createdAt").descending()));

        // 페이지 내 멤버들에 대한 인라인 통계를 batch로 계산 (N+1 방지)
        List<Long> memberIds = memberPage.getContent().stream().map(MemberEntity::getId).toList();
        Map<Long, MemberInlineStats> statsByMember = computeInlineStats(memberIds);

        return memberPage.map(m -> {
            MemberInlineStats s = statsByMember.getOrDefault(m.getId(), MemberInlineStats.EMPTY);
            return AdminMemberResponse.from(m, s.totalSolved, s.totalCorrect, s.activeDays, s.streakDays);
        });
    }

    /** 멤버 인라인 통계 (목록용 — 누적 풀이/정답 수, 풀이 일수, 연속 접속일). */
    private record MemberInlineStats(int totalSolved, int totalCorrect, int activeDays, int streakDays) {
        static final MemberInlineStats EMPTY = new MemberInlineStats(0, 0, 0, 0);
    }

    private Map<Long, MemberInlineStats> computeInlineStats(List<Long> memberIds) {
        if (memberIds.isEmpty()) return Map.of();

        // [member_id, totalCount, correctCount, createdAt] 단일 배치 쿼리
        List<Object[]> rows = solveRepository.findStatsByMemberIds(memberIds);

        Map<Long, Integer> totalsByMember = new HashMap<>();
        Map<Long, Integer> correctsByMember = new HashMap<>();
        // memberId → 풀이가 있었던 LocalDate 집합
        Map<Long, java.util.Set<LocalDate>> datesByMember = new HashMap<>();

        ZoneId zone = ZoneId.systemDefault();
        for (Object[] row : rows) {
            Long memberId = ((Number) row[0]).longValue();
            int totalCount = ((Number) row[1]).intValue();
            int correctCount = ((Number) row[2]).intValue();
            LocalDateTime createdAt = (LocalDateTime) row[3];

            totalsByMember.merge(memberId, totalCount, Integer::sum);
            correctsByMember.merge(memberId, correctCount, Integer::sum);
            datesByMember
                    .computeIfAbsent(memberId, k -> new java.util.HashSet<>())
                    .add(createdAt.atZone(zone).toLocalDate());
        }

        Map<Long, MemberInlineStats> result = new HashMap<>();
        for (Long memberId : memberIds) {
            int total = totalsByMember.getOrDefault(memberId, 0);
            int correct = correctsByMember.getOrDefault(memberId, 0);
            java.util.Set<LocalDate> dates = datesByMember.getOrDefault(memberId, java.util.Set.of());
            int activeDays = dates.size();
            int streak = computeStreakFromDates(dates);
            result.put(memberId, new MemberInlineStats(total, correct, activeDays, streak));
        }
        return result;
    }

    /** 오늘(혹은 어제)부터 거꾸로 세면서 연속 일 수 계산. */
    private int computeStreakFromDates(java.util.Set<LocalDate> dates) {
        if (dates.isEmpty()) return 0;
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /**
     * 특정 멤버의 대시보드 데이터를 한 번에 조회한다.
     * 핵심 지표(누적 풀이, 연속 접속일)를 포함하여 어드민이 사용자 활성도를 즉시 파악 가능.
     */
    public AdminMemberDashboardResponse getDashboard(Long memberId) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));

        List<SolveEntity> solves = solveRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

        // 1) 핵심 지표
        int totalSolved = solves.stream().mapToInt(SolveEntity::getTotalCount).sum();
        int totalCorrect = solves.stream().mapToInt(SolveEntity::getCorrectCount).sum();
        int overallRate = totalSolved > 0 ? Math.round((totalCorrect * 100f) / totalSolved) : 0;
        int streakDays = computeStreakDays(solves);
        Stats stats = new Stats(totalSolved, totalCorrect, overallRate, streakDays, solves.size());

        // 2) 최근 14일 활동
        List<DailyActivity> recentActivity = computeRecentActivity(solves);

        // 3) 과목별 통계 (상위 과목 기준 — leaf subject 의 parent 로 그룹핑)
        List<SubjectStat> subjectStats = computeSubjectStats(solves);

        // 4) 취약 과목 TOP 3 — 기존 WrongAnswerService 재사용
        List<WrongAnswerStatsResponse> wrongStats = wrongAnswerService.getStats(memberId);
        List<WeakSubject> weakSubjects = wrongStats.stream()
                .filter(w -> w.wrongCount() > 0)
                .sorted(Comparator.comparingInt(WrongAnswerStatsResponse::wrongRate).reversed())
                .limit(3)
                .map(w -> new WeakSubject(w.subjectId(), w.subjectName(), w.wrongCount(), w.wrongRate()))
                .toList();

        // 5) 최근 풀이 5건
        List<RecentSolve> recentSolves = solves.stream()
                .limit(5)
                .map(s -> new RecentSolve(
                        s.getId(),
                        s.getCreatedAt(),
                        s.getTotalCount(),
                        s.getCorrectCount(),
                        s.getSubject() != null ? s.getSubject().getId() : null,
                        s.getMockExam() != null ? s.getMockExam().getId() : null))
                .toList();

        MemberInfo memberInfo = new MemberInfo(
                member.getId(),
                member.getNickname(),
                member.getProvider(),
                member.getCreatedAt());

        return new AdminMemberDashboardResponse(
                memberInfo, stats, recentActivity, subjectStats, weakSubjects, recentSolves);
    }

    /**
     * 오늘로부터 거꾸로 세면서 연속으로 풀이가 있는 날짜 수를 센다.
     * 오늘 풀이가 없어도 어제부터 시작하여 연속이면 streak 인정.
     */
    private int computeStreakDays(List<SolveEntity> solves) {
        if (solves.isEmpty()) return 0;
        ZoneId zone = ZoneId.systemDefault();
        java.util.Set<LocalDate> dates = new java.util.HashSet<>();
        for (SolveEntity s : solves) {
            dates.add(s.getCreatedAt().atZone(zone).toLocalDate());
        }
        LocalDate today = LocalDate.now(zone);
        // 오늘 풀이 있으면 오늘부터, 없으면 어제부터 시작
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private List<DailyActivity> computeRecentActivity(List<SolveEntity> solves) {
        ZoneId zone = ZoneId.systemDefault();
        Map<LocalDate, Integer> map = new HashMap<>();
        for (SolveEntity s : solves) {
            LocalDate d = s.getCreatedAt().atZone(zone).toLocalDate();
            map.merge(d, s.getTotalCount(), Integer::sum);
        }
        LocalDate today = LocalDate.now(zone);
        List<DailyActivity> result = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            result.add(new DailyActivity(d.toString(), map.getOrDefault(d, 0)));
        }
        return result;
    }

    private List<SubjectStat> computeSubjectStats(List<SolveEntity> solves) {
        // subjectId가 null인 경우(모의고사 풀이)는 제외
        Map<Long, int[]> agg = new LinkedHashMap<>(); // subjectId → [total, correct]
        Map<Long, String> nameMap = new HashMap<>();
        for (SolveEntity s : solves) {
            if (s.getSubject() == null) continue;
            SubjectEntity leaf = s.getSubject();
            // 상위 과목 표시 (parent 우선)
            SubjectEntity shown = leaf.getParent() != null ? leaf.getParent() : leaf;
            Long sid = shown.getId();
            nameMap.putIfAbsent(sid, shown.getName());
            int[] cur = agg.computeIfAbsent(sid, k -> new int[2]);
            cur[0] += s.getTotalCount();
            cur[1] += s.getCorrectCount();
        }
        List<SubjectStat> result = new ArrayList<>();
        for (Map.Entry<Long, int[]> e : agg.entrySet()) {
            int total = e.getValue()[0];
            int correct = e.getValue()[1];
            int rate = total > 0 ? Math.round((correct * 100f) / total) : 0;
            result.add(new SubjectStat(e.getKey(), nameMap.get(e.getKey()), total, correct, rate));
        }
        // 풀이량 많은 순
        result.sort(Comparator.comparingInt(SubjectStat::total).reversed());
        return result;
    }
}
