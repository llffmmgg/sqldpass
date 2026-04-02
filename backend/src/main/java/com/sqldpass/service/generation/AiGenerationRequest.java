package com.sqldpass.service.generation;

import java.util.List;

public record AiGenerationRequest(String subjectName, long subjectId, List<String> existingSummaries, int count) {
}
