package com.sqldpass.controller.admin.dto;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;

/**
 * 모의고사 생성 요청.
 * - examType 생략 시 SQLD
 * - difficulty 생략 시 NORMAL (정처기에만 적용, SQLD는 무시)
 */
public record CreateMockExamRequest(ExamType examType, MockExamDifficulty difficulty) {
}
