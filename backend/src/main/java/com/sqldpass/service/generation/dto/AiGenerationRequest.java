package com.sqldpass.service.generation.dto;

import java.util.List;

import com.sqldpass.persistent.mockexam.ExamType;

/**
 * AI 문제 생성 요청.
 *
 * - SQLD: subjectName/subjectId/topicName + existingSummaries + count(=3 기본). examType=SQLD.
 * - 정처기: subjectName=카테고리명, topicName=카테고리명, examType=ENGINEER_PRACTICAL,
 *           count=분포 템플릿이 요구한 개수. existingSummaries로 중복 회피.
 */
public record AiGenerationRequest(String subjectName, long subjectId, String topicName,
                                  List<String> existingSummaries, int count, ExamType examType) {

    /** SQLD 기존 호출 호환 (examType=SQLD) */
    public AiGenerationRequest(String subjectName, long subjectId, String topicName,
                               List<String> existingSummaries, int count) {
        this(subjectName, subjectId, topicName, existingSummaries, count, ExamType.SQLD);
    }
}
