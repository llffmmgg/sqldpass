package com.sqldpass.persistent.feedback;

public enum FeedbackStatus {
    /** 새로 들어옴, 미검토 */
    NEW,
    /** 어드민이 확인함 */
    REVIEWED,
    /** 처리 완료 */
    RESOLVED,
    /** 거절/처리 안 함 */
    WONTFIX
}
