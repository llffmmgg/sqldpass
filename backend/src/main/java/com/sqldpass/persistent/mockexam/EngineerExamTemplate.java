package com.sqldpass.persistent.mockexam;

public enum EngineerExamTemplate {

    PROGRAMMING_HEAVY("프로그래밍 편중형"),
    THEORY_HEAVY("이론 편중형"),
    BALANCED("균형형"),
    DB_HEAVY("DB 강조형");

    private final String displayName;

    EngineerExamTemplate(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
