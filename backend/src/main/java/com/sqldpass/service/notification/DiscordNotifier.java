package com.sqldpass.service.notification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.sqldpass.persistent.feedback.FeedbackEntity;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.service.generation.dto.GeneratedQuestion;
import com.sqldpass.service.generation.dto.GenerationResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Discord 웹훅 알림 컴포넌트.
 *
 * 3개 채널로 분리된 알림 전송:
 * - 문제 생성 완료 → generationWebhook
 * - 신규 회원 가입 → signupWebhook
 * - 서버 예외 발생 → errorWebhook
 *
 * 실패해도 본 비즈니스 로직에 영향 주지 않도록 비동기 + try/catch 처리.
 */
@Slf4j
@Component
public class DiscordNotifier {

    private static final int COLOR_SUCCESS = 5763719;   // 초록
    private static final int COLOR_INFO = 5814783;      // 파랑
    private static final int COLOR_ERROR = 15548997;    // 빨강

    private final RestClient restClient;
    private final String generationWebhook;
    private final String signupWebhook;
    private final String errorWebhook;
    private final String feedbackWebhook;

    public DiscordNotifier(
            @Value("${sqldpass.discord.webhook.generation:}") String generationWebhook,
            @Value("${sqldpass.discord.webhook.signup:}") String signupWebhook,
            @Value("${sqldpass.discord.webhook.error:}") String errorWebhook,
            @Value("${sqldpass.discord.webhook.feedback:}") String feedbackWebhook) {
        this.restClient = RestClient.create();
        this.generationWebhook = generationWebhook;
        this.signupWebhook = signupWebhook;
        this.errorWebhook = errorWebhook;
        this.feedbackWebhook = feedbackWebhook;
    }

    // ----------------------------------------------------------
    // AI 문제 생성 완료 알림
    // ----------------------------------------------------------
    public void notifyGenerationComplete(GenerationResult result, List<GeneratedQuestion> savedQuestions) {
        List<DiscordEmbed.Field> fields = new ArrayList<>();
        fields.add(new DiscordEmbed.Field("총 생성", String.valueOf(result.totalGenerated()), true));
        fields.add(new DiscordEmbed.Field("저장 완료", String.valueOf(result.totalSaved()), true));
        fields.add(new DiscordEmbed.Field("에러", String.valueOf(result.errors().size()), true));

        // 저장된 문제 상위 5개 미리보기
        int previewCount = Math.min(5, savedQuestions.size());
        for (int i = 0; i < previewCount; i++) {
            GeneratedQuestion q = savedQuestions.get(i);
            String summary = q.summary() != null ? q.summary() : "(요약 없음)";
            String topic = q.topic() != null ? q.topic() : "-";
            String difficulty = q.difficulty() != null ? difficultyLabel(q.difficulty()) : "-";
            fields.add(new DiscordEmbed.Field(
                    "📝 " + summary,
                    "토픽: " + topic + " · 난이도: " + difficulty,
                    false));
        }

        DiscordEmbed embed = new DiscordEmbed(
                "🤖 AI 문제 생성 완료",
                result.totalSaved() > 0 ? "새 문제가 저장되었습니다." : "저장된 문제가 없습니다.",
                result.totalSaved() > 0 ? COLOR_SUCCESS : COLOR_ERROR,
                fields,
                Instant.now().toString());
        sendAsync(generationWebhook, embed);
    }

    // ----------------------------------------------------------
    // 정처기 모의고사 생성 완료 알림 (generation 채널 재사용)
    // ----------------------------------------------------------
    public void notifyEngineerMockExamGenerated(MockExamEntity exam,
                                                int questionCount,
                                                Map<String, Long> categoryDistribution) {
        List<DiscordEmbed.Field> fields = new ArrayList<>();
        fields.add(new DiscordEmbed.Field("모의고사", exam.getName(), false));
        fields.add(new DiscordEmbed.Field("문항 수", String.valueOf(questionCount), true));
        fields.add(new DiscordEmbed.Field("모의고사 ID", "#" + exam.getId(), true));

        if (categoryDistribution != null && !categoryDistribution.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            categoryDistribution.forEach((category, count) ->
                    sb.append("• ").append(category).append(": ").append(count).append("문항\n"));
            fields.add(new DiscordEmbed.Field("카테고리 분포", sb.toString().trim(), false));
        }

        DiscordEmbed embed = new DiscordEmbed(
                "🛠 정처기 실기 모의고사 생성 완료",
                "새 정처기 모의고사가 생성되었습니다.",
                COLOR_SUCCESS,
                fields,
                Instant.now().toString());
        sendAsync(generationWebhook, embed);
    }

    // ----------------------------------------------------------
    // 신규 회원 가입 알림
    // ----------------------------------------------------------
    public void notifyNewMember(MemberEntity member) {
        DiscordEmbed embed = new DiscordEmbed(
                "👤 신규 가입",
                "새로운 회원이 가입했습니다.",
                COLOR_INFO,
                List.of(
                        new DiscordEmbed.Field("닉네임", member.getNickname(), true),
                        new DiscordEmbed.Field("Provider", member.getProvider(), true),
                        new DiscordEmbed.Field("회원 ID", String.valueOf(member.getId()), true)
                ),
                Instant.now().toString());
        sendAsync(signupWebhook, embed);
    }

    // ----------------------------------------------------------
    // 사용자 피드백 알림
    // ----------------------------------------------------------
    public void notifyFeedback(FeedbackEntity fb, String nickname, String questionSummary) {
        List<DiscordEmbed.Field> fields = new ArrayList<>();
        fields.add(new DiscordEmbed.Field("타입", typeLabel(fb.getType().name()), true));
        fields.add(new DiscordEmbed.Field("작성자", nickname != null ? nickname : "?", true));
        fields.add(new DiscordEmbed.Field("ID", "#" + fb.getId(), true));
        if (fb.getQuestionId() != null) {
            String summary = questionSummary != null ? questionSummary : "(요약 없음)";
            fields.add(new DiscordEmbed.Field("관련 문제", "#" + fb.getQuestionId() + " " + summary, false));
        }
        String content = fb.getContent();
        if (content.length() > 800) {
            content = content.substring(0, 800) + "...";
        }
        fields.add(new DiscordEmbed.Field("내용", content, false));
        if (fb.getPageUrl() != null && !fb.getPageUrl().isBlank()) {
            fields.add(new DiscordEmbed.Field("페이지", fb.getPageUrl(), false));
        }

        DiscordEmbed embed = new DiscordEmbed(
                "📮 새 피드백",
                "사용자가 피드백을 남겼습니다.",
                COLOR_INFO,
                fields,
                Instant.now().toString());
        sendAsync(feedbackWebhook, embed);
    }

    private String typeLabel(String type) {
        return switch (type) {
            case "QUESTION_ERROR" -> "🐞 문제 오류";
            case "BUG" -> "🛠 사이트 버그";
            case "FEATURE" -> "💡 기능 제안";
            case "OTHER" -> "💬 기타";
            default -> type;
        };
    }

    // ----------------------------------------------------------
    // 서버 예외 알림
    // ----------------------------------------------------------
    public void notifyException(Exception ex, String requestPath) {
        // 흔한 노이즈는 알림 제외 (스캐너 봇 트래픽)
        if (ex instanceof NoResourceFoundException) return;
        if (ex instanceof MultipartException) return;

        String stackPreview = stackPreview(ex);
        String message = ex.getMessage() != null ? ex.getMessage() : "(no message)";
        if (message.length() > 500) {
            message = message.substring(0, 500) + "...";
        }

        DiscordEmbed embed = new DiscordEmbed(
                "🚨 서버 예외 발생",
                "```" + ex.getClass().getSimpleName() + "```",
                COLOR_ERROR,
                List.of(
                        new DiscordEmbed.Field("요청 경로", requestPath != null ? requestPath : "-", false),
                        new DiscordEmbed.Field("메시지", message, false),
                        new DiscordEmbed.Field("스택트레이스", "```" + stackPreview + "```", false)
                ),
                Instant.now().toString());
        sendAsync(errorWebhook, embed);
    }

    // ----------------------------------------------------------
    // 내부 유틸
    // ----------------------------------------------------------
    private void sendAsync(String url, DiscordEmbed embed) {
        if (url == null || url.isBlank()) return;
        CompletableFuture.runAsync(() -> {
            try {
                restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("embeds", List.of(embed)))
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception e) {
                log.warn("Discord 알림 전송 실패: {}", e.getMessage());
            }
        });
    }

    private String stackPreview(Exception ex) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stack = ex.getStackTrace();
        int limit = Math.min(5, stack.length);
        for (int i = 0; i < limit; i++) {
            sb.append("at ").append(stack[i].toString()).append("\n");
        }
        String result = sb.toString();
        if (result.length() > 900) {
            result = result.substring(0, 900) + "...";
        }
        return result;
    }

    private String difficultyLabel(Integer difficulty) {
        return switch (difficulty) {
            case 1 -> "기본";
            case 2 -> "심화";
            case 3 -> "고난도";
            default -> String.valueOf(difficulty);
        };
    }
}
