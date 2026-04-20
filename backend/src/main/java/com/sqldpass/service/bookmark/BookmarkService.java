package com.sqldpass.service.bookmark;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.bookmark.dto.BookmarkResponse;
import com.sqldpass.persistent.bookmark.BookmarkEntity;
import com.sqldpass.persistent.bookmark.BookmarkRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

@Service
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final QuestionRepository questionRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository,
                           QuestionRepository questionRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * 즐겨찾기 추가. 이미 있으면 아무 동작도 하지 않음(멱등성).
     * 존재하지 않는 questionId 이면 404.
     */
    @Transactional
    public void add(Long memberId, Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new SqldpassException(ErrorCode.QUESTION_NOT_FOUND);
        }
        if (bookmarkRepository.existsByMemberIdAndQuestionId(memberId, questionId)) {
            return;
        }
        bookmarkRepository.save(new BookmarkEntity(memberId, questionId));
    }

    /** 즐겨찾기 제거. 없으면 아무 동작도 하지 않음. */
    @Transactional
    public void remove(Long memberId, Long questionId) {
        bookmarkRepository.deleteByMemberIdAndQuestionId(memberId, questionId);
    }

    public boolean exists(Long memberId, Long questionId) {
        return bookmarkRepository.existsByMemberIdAndQuestionId(memberId, questionId);
    }

    /**
     * 회원의 전체 즐겨찾기 목록 — 최신 즐겨찾기 순.
     * 북마크 → 질문(+subject) 매핑을 한 번의 findAllById 로 batch 조회 (N+1 방지).
     */
    public List<BookmarkResponse> list(Long memberId) {
        List<BookmarkEntity> bookmarks = bookmarkRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        if (bookmarks.isEmpty()) {
            return List.of();
        }
        List<Long> questionIds = bookmarks.stream().map(BookmarkEntity::getQuestionId).toList();
        Map<Long, QuestionEntity> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(QuestionEntity::getId, Function.identity()));

        return bookmarks.stream()
                .map(b -> {
                    QuestionEntity q = questionMap.get(b.getQuestionId());
                    if (q == null) return null; // question 삭제된 경우 무시
                    return BookmarkResponse.from(b, q);
                })
                .filter(r -> r != null)
                .toList();
    }

    public long count(Long memberId) {
        return bookmarkRepository.countByMemberId(memberId);
    }
}
