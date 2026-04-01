package com.sqldpass.controller.subject;

import java.util.List;

import com.sqldpass.domain.subject.Subject;

public record SubjectResponse(
        Long id,
        String name,
        int displayOrder,
        List<SubjectResponse> children
) {

    public static SubjectResponse from(Subject subject) {
        List<SubjectResponse> children = subject.getChildren().stream()
                .map(SubjectResponse::from)
                .toList();

        return new SubjectResponse(
                subject.getId(),
                subject.getName(),
                subject.getDisplayOrder(),
                children
        );
    }
}
