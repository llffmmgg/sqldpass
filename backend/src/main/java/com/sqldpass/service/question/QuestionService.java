package com.sqldpass.service.question;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.domain.question.Question;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionMapper;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

@Service
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * 무한 풀이용 랜덤 문제 N개 조회.
     * - 로그인 사용자(memberId != null): 사용자가 안 풀었거나 가장 오래 전에 푼 문제 우선
     * - 비로그인: 단순 랜덤
     */
    public List<Question> getRandomQuestions(Long subjectId, Long memberId, int size) {
        if (memberId == null) {
            return questionRepository.findRandomBySubjectId(subjectId, size).stream()
                    .map(QuestionMapper::toDomain)
                    .toList();
        }
        // memberId 기반: 풀에서 푼 문제는 맨 뒤로 밀림
        List<Long> orderedIds = questionRepository.findIdsBySubjectIdRecencyOrdered(subjectId, memberId, size);
        if (orderedIds.isEmpty()) return List.of();
        // findAllById는 순서를 보장하지 않으므로 Map으로 매핑한 뒤 원래 순서대로 재조립
        Map<Long, QuestionEntity> map = new HashMap<>();
        for (QuestionEntity e : questionRepository.findAllById(orderedIds)) {
            map.put(e.getId(), e);
        }
        return orderedIds.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .map(QuestionMapper::toDomain)
                .toList();
    }

    public Question getQuestion(Long id) {
        return questionRepository.findById(id)
                .map(QuestionMapper::toDomain)
                .orElseThrow(() -> new SqldpassException(ErrorCode.QUESTION_NOT_FOUND));
    }
}
