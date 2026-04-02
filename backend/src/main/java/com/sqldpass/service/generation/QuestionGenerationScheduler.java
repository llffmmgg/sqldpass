package com.sqldpass.service.generation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sqldpass.scheduler.enabled", havingValue = "true")
public class QuestionGenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationScheduler.class);

    private final QuestionGenerationService generationService;

    public QuestionGenerationScheduler(QuestionGenerationService generationService) {
        this.generationService = generationService;
    }

    @Scheduled(cron = "${sqldpass.scheduler.generation-cron}")
    public void generateDailyQuestions() {
        log.info("Daily question generation started");
        GenerationResult result = generationService.generateAll();
        log.info("Daily question generation completed: {}", result);
    }
}
