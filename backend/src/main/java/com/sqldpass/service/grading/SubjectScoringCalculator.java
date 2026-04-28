package com.sqldpass.service.grading;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.GradedItem;
import com.sqldpass.controller.publicapi.dto.PastExamPublicDtos.SubjectScore;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.subject.SubjectEntity;

/**
 * 채점 결과를 자격증의 합격 기준 과목 단위로 그룹핑하고 과락/합격 여부를 판정한다.
 *
 * 그룹핑 단위: 문제의 leaf subject 의 parent (없으면 leaf 자체)
 *  → 시드 트리상 정확히 합격 기준 과목 단위와 일치 (예: SQLD subject 5 "SQL 기본" → parent "2과목: SQL 기본 및 활용")
 *
 * 과락 판정: 자격증의 PassFailCriteria.subjectCutoffApplies() 가 true 인 경우만
 *  → 과락 % 미만 정답률 한 과목이라도 있으면 불합격.
 *
 * 합격 판정: 총점 ≥ passScore && 과락 그룹 0개 (단일 과목이면 과락 무시)
 */
public final class SubjectScoringCalculator {

    private SubjectScoringCalculator() {
    }

    public record Outcome(
            List<SubjectScore> subjectScores,
            boolean passed,
            String passReason
    ) {
    }

    public static Outcome compute(ExamType examType,
                                  List<QuestionEntity> questions,
                                  List<GradedItem> gradedItems,
                                  int totalScore) {
        PassFailCriteria criteria = PassFailCriteria.of(examType);

        // questionId → grouping subject 이름
        Map<Long, String> groupNameByQuestion = new LinkedHashMap<>();
        Map<String, Integer> orderBySubject = new LinkedHashMap<>();
        for (QuestionEntity q : questions) {
            SubjectEntity leaf = q.getSubject();
            SubjectEntity group = SubjectGrouping.groupOf(leaf, examType);
            String groupName = group != null ? group.getName() : "기타";
            groupNameByQuestion.put(q.getId(), groupName);
            orderBySubject.putIfAbsent(groupName, orderBySubject.size());
        }

        // 각 그룹별 total/correct 카운트
        Map<String, int[]> agg = new LinkedHashMap<>(); // [total, correct]
        for (GradedItem item : gradedItems) {
            String groupName = groupNameByQuestion.getOrDefault(item.questionId(), "기타");
            int[] counts = agg.computeIfAbsent(groupName, k -> new int[2]);
            counts[0] += 1;
            if (item.correct()) counts[1] += 1;
        }

        List<SubjectScore> scores = new ArrayList<>(agg.size());
        boolean anyFailed = false;
        for (Map.Entry<String, int[]> entry : agg.entrySet()) {
            int total = entry.getValue()[0];
            int correct = entry.getValue()[1];
            double rate = total > 0 ? (correct * 100.0) / total : 0.0;
            int weighted = (int) Math.round(rate); // 100점 만점 환산 = 정답률
            boolean failed = criteria.subjectCutoffApplies() && rate < criteria.subjectCutoffPercent();
            if (failed) anyFailed = true;
            scores.add(new SubjectScore(
                    entry.getKey(),
                    total,
                    correct,
                    Math.round(rate * 10.0) / 10.0,
                    weighted,
                    failed));
        }

        boolean totalOk = totalScore >= criteria.passScore();
        boolean passed = totalOk && !anyFailed;

        String reason;
        if (passed) {
            reason = String.format("합격 (총점 %d점)", totalScore);
        } else if (!totalOk && anyFailed) {
            reason = String.format("불합격 — 총점 %d점 미달 + 과락 과목 있음", totalScore);
        } else if (!totalOk) {
            reason = String.format("불합격 — 총점 %d점 (%d점 이상 필요)", totalScore, criteria.passScore());
        } else {
            String failedNames = scores.stream()
                    .filter(SubjectScore::failed)
                    .map(s -> String.format("%s(%.0f%%)", s.subjectName(), s.rate()))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            reason = String.format("불합격 — 과락 과목: %s", failedNames);
        }

        return new Outcome(scores, passed, reason);
    }
}
