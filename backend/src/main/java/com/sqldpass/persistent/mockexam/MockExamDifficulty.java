package com.sqldpass.persistent.mockexam;

/**
 * 어드민이 모의고사 생성 시 지정하는 평균 난이도.
 *
 * 정처기 실기 모의고사 생성에만 사용. SQLD는 무시.
 *
 * 분포 비율 (20문제 기준 — L1/L2/L3/L4):
 * - EASY:      60 / 30 / 10 / 0   →  12 / 6  / 2  / 0
 * - NORMAL:    20 / 50 / 25 / 5   →  4  / 10 / 5  / 1
 * - HARD:      5  / 25 / 50 / 20  →  1  / 5  / 10 / 4
 * - VERY_HARD: 0  / 10 / 30 / 60  →  0  / 2  / 6  / 12
 */
public enum MockExamDifficulty {
    EASY(1, "쉬움"),
    NORMAL(2, "보통"),
    HARD(3, "어려움"),
    VERY_HARD(4, "매우 어려움");

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
