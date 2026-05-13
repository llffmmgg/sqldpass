package com.sqldpass.service.bookmark;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.bookmark.dto.BookmarkListResponse;
import com.sqldpass.controller.bookmark.dto.BookmarkResponse;
import com.sqldpass.persistent.bookmark.BookmarkEntity;
import com.sqldpass.persistent.bookmark.BookmarkRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.payment.SubscriptionService;

@Service
@Transactional(readOnly = true)
public class BookmarkService {

    /** 무료 / 라이브러리 권한 없는 회원에게 표시되는 최대 즐겨찾기 개수. */
    public static final int FREE_LIMIT = 30;

    private final BookmarkRepository bookmarkRepository;
    private final QuestionRepository questionRepository;
    private final SubscriptionService subscriptionService;

    public BookmarkService(BookmarkRepository bookmarkRepository,
                           QuestionRepository questionRepository,
                           SubscriptionService subscriptionService) {
        this.bookmarkRepository = bookmarkRepository;
        this.questionRepository = questionRepository;
        this.subscriptionService = subscriptionService;
    }

    /**
     * 즐겨찾기 추가. 이미 있으면 아무 동작도 하지 않음(멱등성).
     * 존재하지 않는 questionId 이면 404.
     *
     * 30개 초과 시에도 차단하지 않음 — 백엔드는 그대로 저장, 표시만 잘림.
     * 결제 후 사용자가 자신의 데이터를 그대로 복원할 수 있도록 보존.
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
     * 회원의 즐겨찾기 목록 (최신순) + 권한별 표시 제한 메타.
     *
     * - hasLibraryAccess=true (Thunder/Focus/Pro/Lifetime): 전체 반환, limited=false
     * - hasLibraryAccess=false (무료): 최근 30개만 반환, totalCount 가 30 초과면 limited=true
     *
     * 31번째 이상 데이터는 백엔드에 그대로 보존 — 결제 후 즉시 복원됨.
     */
    public BookmarkListResponse list(Long memberId) {
        boolean unlimited = subscriptionService.hasLibraryAccess(memberId);
        long totalCount = bookmarkRepository.countByMemberId(memberId);

        List<BookmarkEntity> bookmarks = bookmarkRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        List<BookmarkEntity> visible = unlimited
                ? bookmarks
                : bookmarks.stream().limit(FREE_LIMIT).toList();

        boolean limited = !unlimited && totalCount > FREE_LIMIT;

        if (visible.isEmpty()) {
            return new BookmarkListResponse(List.of(), totalCount, limited, FREE_LIMIT);
        }

        List<Long> questionIds = visible.stream().map(BookmarkEntity::getQuestionId).toList();
        Map<Long, QuestionEntity> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(QuestionEntity::getId, Function.identity()));

        List<BookmarkResponse> items = visible.stream()
                .map(b -> {
                    QuestionEntity q = questionMap.get(b.getQuestionId());
                    if (q == null) return null; // question 삭제된 경우 무시
                    return BookmarkResponse.from(b, q);
                })
                .filter(r -> r != null)
                .toList();

        return new BookmarkListResponse(items, totalCount, limited, FREE_LIMIT);
    }

    public long count(Long memberId) {
        return bookmarkRepository.countByMemberId(memberId);
    }
}
