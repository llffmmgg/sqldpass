package com.sqldpass.controller.admin;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqldpass.controller.admin.dto.GenerationResultResponse;
import com.sqldpass.controller.admin.dto.GenerationStatusResponse;
import com.sqldpass.service.generation.GenerationLockService;
import com.sqldpass.service.generation.GenerationResult;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/api/admin/generate/status")
    @Operation(summary = "문제 생성 상태 조회")
    public GenerationStatusResponse getStatus() {
        return new GenerationStatusResponse(lockService.isRunning());
    }

    @PostMapping(value = "/api/admin/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "문제 수동 생성 (SSE)", description = "AI를 이용하여 문제를 생성하고 진행 상황을 실시간으로 전송한다")
    public SseEmitter generate(@RequestParam(defaultValue = "3") int count) {
        SseEmitter emitter = new SseEmitter(600_000L);

        Thread.startVirtualThread(() -> {
            try {
                GenerationResult result = generationService.generateAll(count, event -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(event.type())
                                .data(objectMapper.writeValueAsString(event)));
                    } catch (IOException e) {
                        log.debug("SSE 연결 종료됨 (클라이언트 disconnect)");
                    }
                });

                try {
                    emitter.send(SseEmitter.event()
                            .name("complete")
                            .data(objectMapper.writeValueAsString(GenerationResultResponse.from(result))));
                    emitter.complete();
                } catch (IOException e) {
                    log.debug("SSE 연결 종료됨 (클라이언트 disconnect)");
                }
            } catch (Exception e) {
                log.error("Generation failed", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                }
            }
        });

        return emitter;
    }
}
