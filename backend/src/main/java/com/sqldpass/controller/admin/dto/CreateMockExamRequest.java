package com.sqldpass.controller.admin.dto;

import com.sqldpass.persistent.mockexam.EngineerExamTemplate;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;

/**
 * Mock-exam creation request.
 * - examType defaults to SQLD, difficulty defaults to NORMAL.
 * - engineerTemplate is only used when examType=ENGINEER_PRACTICAL.
 *   null → 4개 템플릿 중 랜덤 선택.
 */
public record CreateMockExamRequest(
        ExamType examType,
        MockExamDifficulty difficulty,
        EngineerExamTemplate engineerTemplate
) {
}
