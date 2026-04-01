package com.sqldpass.controller.common;

public record ErrorResponse(int status, String message, String code) {
}
