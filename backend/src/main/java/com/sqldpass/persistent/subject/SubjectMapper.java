package com.sqldpass.persistent.subject;

import java.util.List;

import com.sqldpass.domain.subject.Subject;

public class SubjectMapper {

    private SubjectMapper() {
    }

    public static Subject toDomain(SubjectEntity entity) {
        Long parentId = entity.getParent() != null ? entity.getParent().getId() : null;

        List<Subject> children = entity.getChildren().stream()
                .map(SubjectMapper::toDomain)
                .toList();

        return new Subject(
                entity.getId(),
                parentId,
                entity.getName(),
                entity.getDisplayOrder(),
                children
        );
    }
}
