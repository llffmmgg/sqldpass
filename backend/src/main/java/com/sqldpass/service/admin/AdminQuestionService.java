package com.sqldpass.service.admin;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.admin.dto.AdminQuestionResponse;
import com.sqldpass.controller.admin.dto.AdminQuestionUpdateRequest;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

@Service
@Transactional(readOnly = true)
public class AdminQuestionService {

    private final QuestionRepository questionRepository;

    public AdminQuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
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
