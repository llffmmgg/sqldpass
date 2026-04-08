package com.sqldpass.service.generation.dto;

import com.sqldpass.persistent.mockexam.ExamType;

public record AiVerificationRequest(ExamType examType, String subjectName, GeneratedQuestion question) {

    public AiVerificationRequest(String subjectName, GeneratedQuestion question) {
        this(ExamType.SQLD, subjectName, question);
    }
}
