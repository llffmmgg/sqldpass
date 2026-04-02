package com.sqldpass.service.generation.dto;

import java.util.List;

public record GenerationResult(int totalGenerated, int totalVerified, int totalSaved, List<String> errors) {
}
