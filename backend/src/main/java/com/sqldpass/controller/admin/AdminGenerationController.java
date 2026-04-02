package com.sqldpass.controller.admin;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.service.generation.QuestionGenerationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "관리자", description = "관리자 API")
@RestController
@RequestMapping("/api/admin")
public class AdminGenerationController {

    private final QuestionGenerationService generationService;

    public AdminGenerationController(QuestionGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/generate")
    @Operation(summary = "문제 수동 생성", description = "AI를 이용하여 모든 리프 과목에 대해 문제를 생성한다")
    public GenerationResultResponse generate() {
        return GenerationResultResponse.from(generationService.generateAll());
    }
}
