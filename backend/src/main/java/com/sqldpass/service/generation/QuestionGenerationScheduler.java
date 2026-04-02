package com.sqldpass.service.generation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "sqldpass.scheduler.enabled", havingValue = "true")
public class QuestionGenerationScheduler {

    private final QuestionGenerationService generationService;

    @Scheduled(cron = "${sqldpass.scheduler.generation-cron}")
    public void generateDailyQuestions() {
        log.info("Daily question generation started");
        GenerationResult result = generationService.generateAll();
        log.info("Daily question generation completed: {}", result);
    }
}
