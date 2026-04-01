package com.sqldpass.persistent.solve;

public interface WrongAnswerStatsProjection {
    Long getSubjectId();
    String getSubjectName();
    int getTotalSolved();
    int getWrongCount();
    int getWrongRate();
}
