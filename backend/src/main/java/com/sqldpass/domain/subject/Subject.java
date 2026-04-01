package com.sqldpass.domain.subject;

import java.util.List;

import lombok.Getter;

@Getter
public class Subject {

    private final Long id;
    private final Long parentId;
    private final String name;
    private final int displayOrder;
    private final List<Subject> children;

    public Subject(Long id, Long parentId, String name, int displayOrder, List<Subject> children) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.displayOrder = displayOrder;
        this.children = children != null ? children : List.of();
    }
}
