package com.sqldpass.service.generation.dto;

import java.util.List;

public record AiGenerationRequest(String subjectName, long subjectId, String topicName,
                                  List<String> existingSummaries, int count) {
}
