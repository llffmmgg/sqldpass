package com.sqldpass.persistent.mockexam;

/**
 * User-selected average difficulty for mock-exam generation.
 * Shared by SQLD, engineer practical, and computer literacy.
 */
public enum MockExamDifficulty {
    EASY(1, "\uC26C\uC6C0"),
    NORMAL(2, "\uBCF4\uD1B5"),
    HARD(3, "\uC5B4\uB824\uC6C0"),
    VERY_HARD(4, "\uB9E4\uC6B0 \uC5B4\uB824\uC6C0");

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
