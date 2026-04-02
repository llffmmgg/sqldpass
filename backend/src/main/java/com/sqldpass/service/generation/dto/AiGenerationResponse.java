package com.sqldpass.service.generation.dto;

import java.util.List;

public record AiGenerationResponse(List<GeneratedQuestion> questions) {
}
