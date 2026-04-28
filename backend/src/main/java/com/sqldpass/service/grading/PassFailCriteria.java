package com.sqldpass.service.grading;

import com.sqldpass.persistent.mockexam.ExamType;

/**
 * 자격증별 공식 합격 기준 / 과목별 과락 룰.
 *
 * 출처:
 * - SQLD / ADsP: dataq.or.kr 공식
 * - 정처기 필기/실기: q-net.or.kr 공식
 * - 컴활 1급/2급: license.korcham.net 공식
 *
 * 모든 자격증 합격선은 60점(평균), 과락 룰 있는 자격증은 모두 과목별 40% 미만.
 * 정처기 실기만 단일 통합 과목이라 과락 비활성.
 */
public enum PassFailCriteria {
    SQLD(60, 40.0, true),
    ENGINEER_WRITTEN(60, 40.0, true),
    ENGINEER_PRACTICAL(60, 0.0, false),
    COMPUTER_LITERACY_1(60, 40.0, true),
    COMPUTER_LITERACY_2(60, 40.0, true),
    ADSP(60, 40.0, true);

    private final int passScore;
    private final double subjectCutoffPercent;
    private final boolean subjectCutoffApplies;

    PassFailCriteria(int passScore, double subjectCutoffPercent, boolean subjectCutoffApplies) {
        this.passScore = passScore;
        this.subjectCutoffPercent = subjectCutoffPercent;
        this.subjectCutoffApplies = subjectCutoffApplies;
    }

    public int passScore() {
        return passScore;
    }

    public double subjectCutoffPercent() {
        return subjectCutoffPercent;
    }

    public boolean subjectCutoffApplies() {
        return subjectCutoffApplies;
    }

    public static PassFailCriteria of(ExamType examType) {
        if (examType == null) return ENGINEER_PRACTICAL;
        return switch (examType) {
            case SQLD -> SQLD;
            case ENGINEER_WRITTEN -> ENGINEER_WRITTEN;
            case ENGINEER_PRACTICAL -> ENGINEER_PRACTICAL;
            case COMPUTER_LITERACY_1 -> COMPUTER_LITERACY_1;
            case COMPUTER_LITERACY_2 -> COMPUTER_LITERACY_2;
            case ADSP -> ADSP;
        };
    }
}
