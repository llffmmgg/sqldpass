package com.sqldpass.persistent.solve;

import java.time.LocalDateTime;

public interface WrongAnswerProjection {
    Long getQuestionId();
    String getQuestionContent();
    String getSubjectName();
    int getWrongCount();
    LocalDateTime getLastWrongAt();
}
