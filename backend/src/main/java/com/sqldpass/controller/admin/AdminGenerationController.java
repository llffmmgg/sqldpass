package com.sqldpass.controller.admin;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqldpass.controller.admin.dto.GenerationResultResponse;
import com.sqldpass.service.generation.GenerationResult;
import com.sqldpass.service.generation.QuestionGenerationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "관리자", description = "관리자 API")
@RestController
@RequestMapping("/api/admin")
public class AdminGenerationController {

    private static final Logger log = LoggerFactory.getLogger(AdminGenerationController.class);

    private final QuestionGenerationService generationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminGenerationController(QuestionGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "문제 수동 생성 (SSE)", description = "AI를 이용하여 문제를 생성하고 진행 상황을 실시간으로 전송한다")
    public SseEmitter generate(@RequestParam(defaultValue = "3") int count) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃

        Thread.startVirtualThread(() -> {
            try {
                GenerationResult result = generationService.generateAll(count, event -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(event.type())
                                .data(objectMapper.writeValueAsString(event)));
                    } catch (IOException e) {
                        log.warn("SSE send failed", e);
                    }
                });

                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(objectMapper.writeValueAsString(GenerationResultResponse.from(result))));
                emitter.complete();
            } catch (Exception e) {
                log.error("Generation failed", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
