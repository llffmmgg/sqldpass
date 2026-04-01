package com.sqldpass.controller.common;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String message, String code, List<FieldError> fieldErrors) {

    public ErrorResponse(int status, String message, String code) {
        this(status, message, code, null);
    }

    public record FieldError(String field, String message) {
    }
}
