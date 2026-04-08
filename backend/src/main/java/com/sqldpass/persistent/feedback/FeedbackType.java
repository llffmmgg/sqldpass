package com.sqldpass.persistent.feedback;

public enum FeedbackType {
    /** 특정 문제의 오류 신고 (정답·해설·오타 등) */
    QUESTION_ERROR,
    /** 사이트 버그 */
    BUG,
    /** 기능 제안 */
    FEATURE,
    /** 기타 의견 */
    OTHER
}
