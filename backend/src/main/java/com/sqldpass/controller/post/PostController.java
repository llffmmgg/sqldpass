package com.sqldpass.controller.post;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.post.dto.PostDtos.CommentRequest;
import com.sqldpass.controller.post.dto.PostDtos.CommentResponse;
import com.sqldpass.controller.post.dto.PostDtos.PostDetailResponse;
import com.sqldpass.controller.post.dto.PostDtos.PostEditRequest;
import com.sqldpass.controller.post.dto.PostDtos.PostPageResponse;
import com.sqldpass.controller.post.dto.PostDtos.PostSubmitRequest;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.post.PostCategory;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.post.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "게시판", description = "게시판 (합격 후기 등) 사용자 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    @Operation(summary = "게시판 목록 (PUBLISHED 만, 카테고리/자격증 필터)")
    public PostPageResponse list(
            @RequestParam(defaultValue = "PASS_REVIEW") PostCategory category,
            @RequestParam(required = false) ExamType cert,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postService.listPublic(category, cert, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "게시글 상세 (PENDING 은 작성자만)")
    public PostDetailResponse get(@PathVariable Long id, HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        return postService.getDetail(id, memberId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "게시글 작성 (PASS_REVIEW 는 PENDING 으로 저장)")
    public Long submit(HttpServletRequest request, @Valid @RequestBody PostSubmitRequest body) {
        Long memberId = requireMember(request);
        return postService.submit(memberId, body);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "게시글 수정 (작성자 본인만)")
    public void edit(@PathVariable Long id,
                     HttpServletRequest request,
                     @Valid @RequestBody PostEditRequest body) {
        Long memberId = requireMember(request);
        postService.edit(id, memberId, body);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "게시글 삭제 (작성자 본인만)")
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        Long memberId = requireMember(request);
        postService.deleteByOwner(id, memberId);
    }

    // 댓글
    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "댓글 작성")
    public CommentResponse addComment(@PathVariable Long id,
                                      HttpServletRequest request,
                                      @Valid @RequestBody CommentRequest body) {
        Long memberId = requireMember(request);
        return postService.addComment(id, memberId, body);
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "댓글 삭제 (작성자 본인만)")
    public void deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long memberId = requireMember(request);
        postService.deleteComment(commentId, memberId);
    }

    /** OptionalMemberAuthInterceptor 가 토큰만 옵셔널 주입하므로 쓰기 메서드에서는 필수 체크. */
    private static Long requireMember(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        return memberId;
    }
}
