package com.sqldpass.service.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.admin.dto.AdminQuestionResponse;
import com.sqldpass.controller.admin.dto.AdminQuestionUpdateRequest;
import com.sqldpass.controller.admin.dto.QuestionVerifyResultResponse;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.generation.AiProvider;
import com.sqldpass.service.generation.dto.AiVerificationRequest;
import com.sqldpass.service.generation.dto.AiVerificationResponse;
import com.sqldpass.service.generation.dto.GeneratedQuestion;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminQuestionService {

    private final QuestionRepository questionRepository;
    private final AiProvider verifier;

    public AdminQuestionService(QuestionRepository questionRepository,
                                @Qualifier("verifier") AiProvider verifier) {
        this.questionRepository = questionRepository;
        this.verifier = verifier;
    }

    /**
     * 의심 문제 일괄 LLM 검증.
     * subjectId가 주어지면 그 과목만, 아니면 전체.
     * limit으로 비용 폭주 방지 (0이면 전체).
     * approved=false인 ID + 사유만 반환.
     */
    public List<QuestionVerifyResultResponse> verifyAll(Long subjectId, int limit) {
        List<QuestionEntity> questions;
        if (subjectId != null) {
            questions = questionRepository
                    .findBySubjectIdWithSubject(subjectId, PageRequest.of(0, limit > 0 ? limit : 10000))
                    .getContent();
        } else {
            questions = questionRepository
                    .findAllWithSubject(PageRequest.of(0, limit > 0 ? limit : 10000))
                    .getContent();
        }

        List<QuestionVerifyResultResponse> suspicious = new ArrayList<>();
        int processed = 0;
        for (QuestionEntity q : questions) {
            try {
                GeneratedQuestion gq = new GeneratedQuestion(
                        q.getContent(),
                        q.getCorrectOption() != null ? q.getCorrectOption() : 1,
                        q.getExplanation(),
                        q.getSummary(),
                        q.getTopic(),
                        q.getDifficulty(),
                        q.getQuestionType() != null ? q.getQuestionType().name() : null,
                        q.getAnswer(),
                        null);
                AiVerificationResponse resp = verifier.verifyQuestion(
                        new AiVerificationRequest(q.getSubject().getName(), gq));
                if (!resp.approved()) {
                    suspicious.add(new QuestionVerifyResultResponse(
                            q.getId(), q.getSubject().getName(), q.getSummary(), resp.reason()));
                }
            } catch (Exception e) {
                log.warn("문제 #{} 검증 실패: {}", q.getId(), e.getMessage());
                suspicious.add(new QuestionVerifyResultResponse(
                        q.getId(), q.getSubject().getName(), q.getSummary(), "검증 호출 실패: " + e.getMessage()));
            }
            processed++;
            if (processed % 20 == 0) {
                log.info("LLM 일괄 검증 진행 {}/{}", processed, questions.size());
            }
        }
        log.info("LLM 일괄 검증 완료 - 검사 {}개, 의심 {}개", processed, suspicious.size());
        return suspicious;
    }

    public Page<AdminQuestionResponse> getQuestions(Long subjectId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (subjectId != null) {
            return questionRepository.findBySubjectIdWithSubject(subjectId, pageable)
                    .map(AdminQuestionResponse::from);
        }
        return questionRepository.findAllWithSubject(pageable)
                .map(AdminQuestionResponse::from);
    }

    public AdminQuestionResponse getQuestion(Long id) {
        QuestionEntity entity = questionRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.QUESTION_NOT_FOUND));
        return AdminQuestionResponse.from(entity);
    }

    @Transactional
    public AdminQuestionResponse updateQuestion(Long id, AdminQuestionUpdateRequest request) {
        QuestionEntity entity = questionRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.QUESTION_NOT_FOUND));
        entity.update(request.content(), request.correctOption(), request.explanation(), request.summary());
        return AdminQuestionResponse.from(entity);
    }

    @Transactional
    public void deleteQuestion(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new SqldpassException(ErrorCode.QUESTION_NOT_FOUND);
        }
        questionRepository.deleteById(id);
    }

    public long countAll() {
        return questionRepository.count();
    }

    public long countToday() {
        return questionRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay());
    }
}
