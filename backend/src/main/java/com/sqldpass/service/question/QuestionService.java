package com.sqldpass.service.question;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.domain.question.Question;
import com.sqldpass.persistent.question.QuestionMapper;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.service.common.NotFoundException;

@Service
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<Question> getRandomQuestions(Long subjectId, int size) {
        return questionRepository.findRandomBySubjectId(subjectId, size).stream()
                .map(QuestionMapper::toDomain)
                .toList();
    }

    public Question getQuestion(Long id) {
        return questionRepository.findById(id)
                .map(QuestionMapper::toDomain)
                .orElseThrow(() -> new NotFoundException("QUESTION_NOT_FOUND", "문제를 찾을 수 없습니다."));
    }
}
