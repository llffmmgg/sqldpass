package com.sqldpass.service.content;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.content.dto.ContentSnapshotResponse;
import com.sqldpass.controller.content.dto.ContentSnapshotResponse.MockExamSnapshot;
import com.sqldpass.controller.content.dto.ContentSnapshotResponse.QuestionSnapshot;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
import com.sqldpass.persistent.payment.MockExamPurchaseRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.service.payment.SubscriptionService;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentSnapshotService {

    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final SubscriptionService subscriptionService;
    private final MockExamPurchaseRepository mockExamPurchaseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String currentVersion() {
        LocalDateTime mockExamMax = mockExamRepository.findSnapshotMaxUpdatedAt().orElse(EPOCH);
        LocalDateTime questionMax = questionRepository.findSnapshotMaxUpdatedAt().orElse(EPOCH);
        LocalDateTime maxTs = mockExamMax.isAfter(questionMax) ? mockExamMax : questionMax;
        long mockExamCount = mockExamRepository.count();
        long questionCount = questionRepository.count();
        return maxTs + "-" + mockExamCount + "-" + questionCount;
    }

    public String currentMobileVersion(Long memberId) {
        boolean premiumAccess = subscriptionService.hasPremiumAccess(memberId);
        List<Long> purchased = memberId == null
                ? Collections.emptyList()
                : mockExamPurchaseRepository.findMockExamIdsByMemberId(memberId);
        String purchaseVersion = purchased.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining("."));
        return currentVersion()
                + "-mobile-"
                + (memberId != null ? memberId : "anonymous")
                + "-"
                + premiumAccess
                + "-"
                + purchaseVersion;
    }

    public ContentSnapshotResponse buildSnapshot() {
        List<MockExamEntity> mockExams = loadSnapshotExams();
        return toResponse(currentVersion(), mockExams);
    }

    public ContentSnapshotResponse buildMobileSnapshot(Long memberId) {
        boolean premiumAccess = subscriptionService.hasPremiumAccess(memberId);
        List<Long> purchased = memberId == null
                ? Collections.emptyList()
                : mockExamPurchaseRepository.findMockExamIdsByMemberId(memberId);
        List<MockExamEntity> mockExams = loadSnapshotExams().stream()
                .filter(exam -> isMobileVisible(exam, premiumAccess, purchased))
                .toList();
        return toResponse(currentMobileVersion(memberId), mockExams);
    }

    private List<MockExamEntity> loadSnapshotExams() {
        List<MockExamEntity> mockExams = new ArrayList<>(mockExamRepository.findAllForSnapshot());
        mockExams.sort(Comparator
                .comparing(MockExamEntity::getExamType, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                        m -> m.getExamYear() != null ? m.getExamYear() : 0,
                        Comparator.reverseOrder())
                .thenComparing(
                        m -> m.getExamRound() != null ? m.getExamRound() : 0,
                        Comparator.reverseOrder())
                .thenComparing(MockExamEntity::getSequence, Comparator.reverseOrder()));
        return mockExams;
    }

    private ContentSnapshotResponse toResponse(String version, List<MockExamEntity> mockExams) {
        List<MockExamSnapshot> mockExamDtos = new ArrayList<>(mockExams.size());
        int totalQuestions = 0;
        for (MockExamEntity exam : mockExams) {
            List<QuestionEntity> questions = new ArrayList<>(exam.getQuestions());
            questions.sort(Comparator.comparing(
                    q -> q.getDisplayOrder() != null ? q.getDisplayOrder() : Integer.MAX_VALUE));
            List<QuestionSnapshot> questionDtos = new ArrayList<>(questions.size());
            for (QuestionEntity q : questions) {
                questionDtos.add(toQuestionDto(q));
            }
            totalQuestions += questionDtos.size();
            mockExamDtos.add(new MockExamSnapshot(
                    exam.getId(),
                    exam.getName(),
                    exam.getExamType() != null ? exam.getExamType().name() : null,
                    exam.getSequence(),
                    exam.getVisibility() != null ? exam.getVisibility().name() : null,
                    exam.isExpertVerified(),
                    exam.getKind() != null ? exam.getKind().name() : null,
                    exam.getExamYear(),
                    exam.getExamRound(),
                    exam.getExamDate(),
                    exam.getTemplate() != null ? exam.getTemplate().name() : null,
                    questionDtos));
        }

        return new ContentSnapshotResponse(
                version,
                LocalDateTime.now(),
                mockExamDtos.size(),
                totalQuestions,
                mockExamDtos);
    }

    private boolean isMobileVisible(
            MockExamEntity exam,
            boolean premiumAccess,
            List<Long> purchased) {
        if (exam.getVisibility() != MockExamVisibility.PREMIUM) {
            return true;
        }
        return premiumAccess || purchased.contains(exam.getId());
    }

    private QuestionSnapshot toQuestionDto(QuestionEntity q) {
        SubjectEntity subject = q.getSubject();
        String subjectName = subject != null ? subject.getName() : null;
        String parentName = (subject != null && subject.getParent() != null)
                ? subject.getParent().getName()
                : null;
        Long subjectId = subject != null ? subject.getId() : null;
        return new QuestionSnapshot(
                q.getId(),
                q.getDisplayOrder(),
                subjectId,
                subjectName,
                parentName,
                q.getContent(),
                q.getQuestionType() != null ? q.getQuestionType().name() : "MCQ",
                q.getCorrectOption(),
                q.getAnswer(),
                parseKeywords(q.getKeywords()),
                q.getExplanation(),
                q.getSummary(),
                q.getTopic(),
                q.getDifficulty());
    }

    private List<String> parseKeywords(String keywordsJson) {
        if (keywordsJson == null || keywordsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return Optional.ofNullable(
                    objectMapper.readValue(keywordsJson, new TypeReference<List<String>>() {}))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
