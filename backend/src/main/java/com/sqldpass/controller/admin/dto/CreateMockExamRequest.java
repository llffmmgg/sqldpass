package com.sqldpass.controller.admin.dto;

import com.sqldpass.persistent.mockexam.ExamType;

/** 모의고사 생성 요청 — examType 생략 시 SQLD. */
public record CreateMockExamRequest(ExamType examType) {
}
