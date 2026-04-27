package com.sqldpass.service.post;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.post.dto.PostDtos.CommentRequest;
import com.sqldpass.controller.post.dto.PostDtos.CommentResponse;
import com.sqldpass.controller.post.dto.PostDtos.PostDetailResponse;
import com.sqldpass.controller.post.dto.PostDtos.PostEditRequest;
import com.sqldpass.controller.post.dto.PostDtos.PostPageResponse;
import com.sqldpass.controller.post.dto.PostDtos.PostSubmitRequest;
import com.sqldpass.controller.post.dto.PostDtos.PostSummaryResponse;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.post.PostCategory;
import com.sqldpass.persistent.post.PostCommentEntity;
import com.sqldpass.persistent.post.PostCommentRepository;
import com.sqldpass.persistent.post.PostEntity;
import com.sqldpass.persistent.post.PostRepository;
import com.sqldpass.persistent.post.PostStatus;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;

/**
 * 게시판 비즈니스 로직.
 * - 합격 후기(PASS_REVIEW) 는 PENDING 으로 저장 → 어드민 승인 후 PUBLISHED
 * - 추후 NOTICE/QNA 등 카테고리 추가 시 신규 카테고리는 PUBLISHED 즉시로 분기 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final MemberRepository memberRepository;

    // ========== 사용자 — 목록/상세 ==========

    public PostPageResponse listPublic(PostCategory category, ExamType cert, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<PostEntity> p = postRepository.findPublic(
                PostStatus.PUBLISHED, category, cert,
                PageRequest.of(Math.max(page, 0), safeSize));

        // N+1 방지: 한 번에 commentCount 조회
        List<Long> ids = p.getContent().stream().map(PostEntity::getId).toList();
        Map<Long, Integer> commentCountMap = countCommentsBatch(ids);

        List<PostSummaryResponse> items = p.getContent().stream()
                .map(pe -> PostSummaryResponse.from(pe, commentCountMap.getOrDefault(pe.getId(), 0)))
                .toList();

        return new PostPageResponse(items, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    private Map<Long, Integer> countCommentsBatch(List<Long> postIds) {
        if (postIds.isEmpty()) return Map.of();
        // 단순화: 각 post 마다 size 조회. 양 적으면 OK. 트래픽 늘면 GROUP BY 쿼리로 변경.
        return postIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> commentRepository.findByPostIdOrderByCreatedAtAsc(id).size()
        ));
    }

    @Transactional
    public PostDetailResponse getDetail(Long id, Long viewerMemberId) {
        PostEntity p = postRepository.findByIdWithMember(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.POST_NOT_FOUND));

        // PENDING 글은 작성자 본인 또는 어드민만 열람 가능
        if (p.getStatus() != PostStatus.PUBLISHED) {
            if (viewerMemberId == null || !viewerMemberId.equals(p.getMember().getId())) {
                throw new SqldpassException(ErrorCode.POST_NOT_FOUND);
            }
        }

        // 조회수 증가 — 본인 조회는 카운트 X
        if (viewerMemberId == null || !viewerMemberId.equals(p.getMember().getId())) {
            p.incrementView();
        }

        List<PostCommentEntity> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(id);
        return PostDetailResponse.from(p, comments);
    }

    // ========== 사용자 — 제출/수정/삭제 ==========

    @Transactional
    public Long submit(Long memberId, PostSubmitRequest req) {
        MemberEntity member = loadMember(memberId);

        // PASS_REVIEW 는 cert 필수
        if (req.category() == PostCategory.PASS_REVIEW && req.cert() == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "합격 후기는 자격증을 선택해야 합니다.");
        }

        // 카테고리별 초기 상태: PASS_REVIEW 는 어드민 승인 필요 → PENDING
        PostStatus initialStatus = (req.category() == PostCategory.PASS_REVIEW)
                ? PostStatus.PENDING
                : PostStatus.PUBLISHED;

        PostEntity p = new PostEntity(member, req.category(), initialStatus,
                req.cert(), req.title().trim(), req.content().trim());
        postRepository.save(p);
        return p.getId();
    }

    @Transactional
    public void edit(Long id, Long memberId, PostEditRequest req) {
        PostEntity p = postRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.POST_NOT_FOUND));
        if (!p.getMember().getId().equals(memberId)) {
            throw new SqldpassException(ErrorCode.FORBIDDEN);
        }
        p.edit(req.title().trim(), req.content().trim());
    }

    @Transactional
    public void deleteByOwner(Long id, Long memberId) {
        PostEntity p = postRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.POST_NOT_FOUND));
        if (!p.getMember().getId().equals(memberId)) {
            throw new SqldpassException(ErrorCode.FORBIDDEN);
        }
        postRepository.delete(p);
    }

    // ========== 댓글 ==========

    @Transactional
    public CommentResponse addComment(Long postId, Long memberId, CommentRequest req) {
        PostEntity p = postRepository.findById(postId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.POST_NOT_FOUND));
        if (p.getStatus() != PostStatus.PUBLISHED) {
            throw new SqldpassException(ErrorCode.POST_NOT_FOUND);
        }
        MemberEntity member = loadMember(memberId);
        PostCommentEntity c = new PostCommentEntity(p, member, req.content().trim());
        commentRepository.save(c);
        return CommentResponse.from(c);
    }

    @Transactional
    public void deleteComment(Long commentId, Long memberId) {
        PostCommentEntity c = commentRepository.findById(commentId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.COMMENT_NOT_FOUND));
        if (!c.getMember().getId().equals(memberId)) {
            throw new SqldpassException(ErrorCode.FORBIDDEN);
        }
        commentRepository.delete(c);
    }

    // ========== 어드민 ==========

    public List<PostSummaryResponse> listPending() {
        return listForAdmin(PostStatus.PENDING);
    }

    /**
     * 어드민용 게시글 목록.
     * @param status null 이면 전체, 값이 있으면 해당 status 만
     */
    public List<PostSummaryResponse> listForAdmin(PostStatus status) {
        List<PostEntity> rows = (status == null)
                ? postRepository.findAllForAdmin()
                : postRepository.findByStatusOrderByCreatedAtDesc(status);
        Map<Long, Integer> commentCountMap = countCommentsBatch(rows.stream().map(PostEntity::getId).toList());
        return rows.stream()
                .map(p -> PostSummaryResponse.from(p, commentCountMap.getOrDefault(p.getId(), 0)))
                .toList();
    }

    public PostDetailResponse getForAdmin(Long id) {
        PostEntity p = postRepository.findByIdWithMember(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.POST_NOT_FOUND));
        List<PostCommentEntity> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(id);
        return PostDetailResponse.from(p, comments);
    }

    @Transactional
    public void approve(Long id) {
        PostEntity p = postRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.POST_NOT_FOUND));
        p.approve();
    }

    /** 어드민이 본문/제목 직접 수정 (작성자 확인 없이 권한). */
    @Transactional
    public void editByAdmin(Long id, PostEditRequest req) {
        PostEntity p = postRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.POST_NOT_FOUND));
        p.edit(req.title().trim(), req.content().trim());
    }

    @Transactional
    public void deleteByAdmin(Long id) {
        PostEntity p = postRepository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.POST_NOT_FOUND));
        postRepository.delete(p);
    }

    /** 어드민이 임의 댓글 삭제 (작성자 무관 권한). */
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        PostCommentEntity c = commentRepository.findById(commentId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.COMMENT_NOT_FOUND));
        commentRepository.delete(c);
    }

    // ========== 헬퍼 ==========

    private MemberEntity loadMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
