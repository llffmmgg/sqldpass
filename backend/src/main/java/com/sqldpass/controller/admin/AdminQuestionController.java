package com.sqldpass.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "관리자 - 문제", description = "문제 관리 API")
@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {

    private final QuestionRepository questionRepository;

    public AdminQuestionController(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @GetMapping
    @Operation(summary = "문제 목록 조회")
    public Page<AdminQuestionResponse> getQuestions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (subjectId != null) {
            return questionRepository.findBySubjectIdOrderByCreatedAtDesc(subjectId, pageable)
                    .map(AdminQuestionResponse::from);
        }
        return questionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AdminQuestionResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "문제 상세 조회")
    public AdminQuestionResponse getQuestion(@PathVariable Long id) {
        QuestionEntity entity = questionRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.QUESTION_NOT_FOUND));
        return AdminQuestionResponse.from(entity);
    }

    @PutMapping("/{id}")
    @Operation(summary = "문제 수정")
    public AdminQuestionResponse updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody AdminQuestionUpdateRequest request) {
        QuestionEntity entity = questionRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.QUESTION_NOT_FOUND));
        entity.update(request.content(), request.correctOption(), request.explanation(), request.summary());
        questionRepository.save(entity);
        return AdminQuestionResponse.from(entity);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "문제 삭제")
    public void deleteQuestion(@PathVariable Long id) {
        if (!questionRepository.existsById(id)) {
            throw new SqldpassException(ErrorCode.QUESTION_NOT_FOUND);
        }
        questionRepository.deleteById(id);
    }
}
