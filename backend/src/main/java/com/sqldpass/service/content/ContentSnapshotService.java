package com.sqldpass.service.content;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.content.dto.ContentSnapshotResponse;
import com.sqldpass.controller.content.dto.ContentSnapshotResponse.MockExamSnapshot;
import com.sqldpass.controller.content.dto.ContentSnapshotResponse.QuestionSnapshot;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 안드로이드 앱 prefetch 용 콘텐츠 스냅샷.
 * 한 GET 요청으로 전체 회차+문제를 반환하므로 트래픽 비용은 첫 부트(또는 ETag 변경 시)에만 발생한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentSnapshotService {

    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);
    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * version 만 빠르게 계산 — If-None-Match 304 분기에 사용. 본문을 만들지 않으므로 비용이 매우 적다.
     */
    public String currentVersion() {
        LocalDateTime mockExamMax = mockExamRepository.findSnapshotMaxUpdatedAt().orElse(EPOCH);
        LocalDateTime questionMax = questionRepository.findSnapshotMaxUpdatedAt().orElse(EPOCH);
        LocalDateTime maxTs = mockExamMax.isAfter(questionMax) ? mockExamMax : questionMax;
        long mockExamCount = mockExamRepository.count();
        long questionCount = questionRepository.count();
        // version = max(updatedAt) + total counts. 콘텐츠 변동(추가/삭제/수정)을 모두 포착한다.
        return maxTs.toString() + "-" + mockExamCount + "-" + questionCount;
    }

    public ContentSnapshotResponse buildSnapshot() {
        // 정렬 가능한 mutable 사본 — repo가 반환하는 컬렉션 종류에 의존하지 않는다.
        List<MockExamEntity> mockExams = new ArrayList<>(mockExamRepository.findAllForSnapshot());
        // sequence/examYear 내림차순으로 안정 정렬 — 클라이언트 화면 정렬과 일치.
        mockExams.sort(Comparator
                .comparing(MockExamEntity::getExamType, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                        m -> m.getExamYear() != null ? m.getExamYear() : 0,
                        Comparator.reverseOrder())
                .thenComparing(
                        m -> m.getExamRound() != null ? m.getExamRound() : 0,
                        Comparator.reverseOrder())
                .thenComparing(MockExamEntity::getSequence, Comparator.reverseOrder()));

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
                currentVersion(),
                LocalDateTime.now(),
                mockExamDtos.size(),
                totalQuestions,
                mockExamDtos);
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
