package com.sqldpass.controller.admin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.admin.dto.GenerationStatusResponse;
import com.sqldpass.service.generation.GenerationLockService;
import com.sqldpass.service.generation.QuestionGenerationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "관리자", description = "관리자 API")
@RestController
@RequiredArgsConstructor
public class AdminGenerationController {

    private final QuestionGenerationService generationService;
    private final GenerationLockService lockService;

    @GetMapping("/api/admin/generate/status")
    @Operation(summary = "문제 생성 상태 조회")
    public GenerationStatusResponse getStatus() {
        return lockService.getStatus();
    }

    @PostMapping("/api/admin/generate/reset")
    @Operation(summary = "생성 상태 초기화 (IDLE로 리셋)")
    public void reset() {
        lockService.reset();
    }

    @PostMapping("/api/admin/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "문제 생성 시작 (백그라운드)")
    public void generate(@RequestParam(defaultValue = "3") int count) {
        Thread.startVirtualThread(() -> {
            try {
                generationService.generateAll(count, event ->
                        log.info("Generation: {}", event.message()));
            } catch (Exception e) {
                log.error("Background generation failed", e);
            }
        });
    }
}
