package com.sqldpass.persistent.mockexam;

/**
 * 어드민이 모의고사 생성 시 지정하는 평균 난이도.
 *
 * 정처기 실기 모의고사 생성에만 사용. SQLD는 무시.
 *
 * 분포 비율 (20문제 기준):
 * - EASY:   L1 60% / L2 30% / L3 10%  → 12 / 6 / 2
 * - NORMAL: L1 25% / L2 50% / L3 25%  → 5  / 10 / 5
 * - HARD:   L1 10% / L2 30% / L3 60%  → 2  / 6  / 12
 */
public enum MockExamDifficulty {
    EASY(1, "쉬움"),
    NORMAL(2, "보통"),
    HARD(3, "어려움");

    private final int level;
    private final String label;

    MockExamDifficulty(int level, String label) {
        this.level = level;
        this.label = label;
    }

    public int level() {
        return level;
    }

    public String label() {
        return label;
    }
}
