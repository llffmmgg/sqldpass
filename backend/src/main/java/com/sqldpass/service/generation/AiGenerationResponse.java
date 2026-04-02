package com.sqldpass.service.generation;

import java.util.List;

public record AiGenerationResponse(List<GeneratedQuestion> questions) {
}
