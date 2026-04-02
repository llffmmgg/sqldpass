package com.sqldpass.controller.admin.dto;

import java.util.List;

import com.sqldpass.service.generation.GenerationResult;

public record GenerationResultResponse(int totalGenerated, int totalVerified, int totalSaved, List<String> errors) {

    public static GenerationResultResponse from(GenerationResult result) {
        return new GenerationResultResponse(
                result.totalGenerated(), result.totalVerified(), result.totalSaved(), result.errors());
    }
}
