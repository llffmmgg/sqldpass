package com.sqldpass.service.post;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.controller.post.dto.PostDtos.PostDetailResponse;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.post.PostCategory;
import com.sqldpass.persistent.post.PostCommentRepository;
import com.sqldpass.persistent.post.PostEntity;
import com.sqldpass.persistent.post.PostRepository;
import com.sqldpass.persistent.post.PostStatus;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * PostService.getDetail 의 native UPDATE 흐름 검증.
 * - 비작성자 viewer → incrementViewCount 1회 호출 + entity in-memory +1 동기화
 * - 작성자 본인 viewer → increment 호출 0회
 * - PENDING 글에 비작성자 viewer → POST_NOT_FOUND, increment 호출 0회
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostCommentRepository commentRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("getDetail PUBLISHED + 비작성자 viewer — native increment + in-memory 동기화")
    void getDetail_published_nonAuthor_incrementsBoth() {
        MemberEntity author = new MemberEntity("kakao", "k1", "글쓴이");
        setMemberId(author, 1L);
        PostEntity post = new PostEntity(author, PostCategory.PASS_REVIEW,
                PostStatus.PUBLISHED, null, "제목", "본문");
        setPostId(post, 100L);
        // 시작 viewCount=0 → 비작성자가 들어오면 응답 viewCount=1 이어야 함
        given(postRepository.findByIdWithMember(100L)).willReturn(Optional.of(post));
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(100L)).willReturn(List.of());

        PostDetailResponse response = postService.getDetail(100L, 999L); // 다른 회원

        then(postRepository).should().incrementViewCount(100L);
        assertThat(response.viewCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getDetail PUBLISHED + 작성자 본인 viewer — increment 호출 안 함")
    void getDetail_published_author_skipsIncrement() {
        MemberEntity author = new MemberEntity("kakao", "k1", "글쓴이");
        setMemberId(author, 1L);
        PostEntity post = new PostEntity(author, PostCategory.PASS_REVIEW,
                PostStatus.PUBLISHED, null, "제목", "본문");
        setPostId(post, 100L);
        given(postRepository.findByIdWithMember(100L)).willReturn(Optional.of(post));
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(100L)).willReturn(List.of());

        PostDetailResponse response = postService.getDetail(100L, 1L); // 작성자 본인

        then(postRepository).should(never()).incrementViewCount(100L);
        assertThat(response.viewCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getDetail PENDING + 비작성자 viewer — POST_NOT_FOUND, increment 호출 안 함")
    void getDetail_pending_nonAuthor_throws() {
        MemberEntity author = new MemberEntity("kakao", "k1", "글쓴이");
        setMemberId(author, 1L);
        PostEntity post = new PostEntity(author, PostCategory.PASS_REVIEW,
                PostStatus.PENDING, null, "제목", "본문");
        setPostId(post, 100L);
        given(postRepository.findByIdWithMember(100L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.getDetail(100L, 999L))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        then(postRepository).should(never()).incrementViewCount(100L);
    }

    private static void setMemberId(MemberEntity entity, Long id) {
        try {
            var field = MemberEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setPostId(PostEntity entity, Long id) {
        try {
            var field = PostEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
