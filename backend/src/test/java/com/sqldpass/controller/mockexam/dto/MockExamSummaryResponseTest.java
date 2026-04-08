package com.sqldpass.controller.mockexam.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.persistent.mockexam.ExamType;

class MockExamSummaryResponseTest {

    @Test
    void sqldDifficultyUsesFourLevelScale() {
        MockExam exam = new MockExam(
                1L,
                "SQLD mock",
                ExamType.SQLD,
                1,
                LocalDateTime.now(),
                50,
                1.2,
                1,
                2
        );

        MockExamSummaryResponse response = MockExamSummaryResponse.from(exam);

        assertEquals("\uC26C\uC6C0", response.difficultyLabel());
    }

    @Test
    void computerLiteracyDifficultyStillMapsCorrectly() {
        MockExam exam = new MockExam(
                2L,
                "CL mock",
                ExamType.COMPUTER_LITERACY_1,
                1,
                LocalDateTime.now(),
                60,
                3.4,
                2,
                4
        );

        MockExamSummaryResponse response = MockExamSummaryResponse.from(exam);

        assertEquals("\uB9E4\uC6B0 \uC5B4\uB824\uC6C0", response.difficultyLabel());
    }
}
