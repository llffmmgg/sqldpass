package com.sqldpass.controller.admin.dto;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;

/**
 * Mock-exam creation request.
 * examType defaults to SQLD and difficulty defaults to NORMAL.
 */
public record CreateMockExamRequest(ExamType examType, MockExamDifficulty difficulty) {
}
