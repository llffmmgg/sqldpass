package com.sqldpass.controller.admin.dto;

import java.time.LocalDateTime;

public record GenerationStatusResponse(String status, String result, LocalDateTime startedAt) {
}
