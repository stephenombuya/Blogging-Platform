package com.blogplatform.service;

import com.blogplatform.dto.CommentDto;
import com.blogplatform.exception.AccessDeniedException;
import com.blogplatform.exception.BadRequestException;
import com.blogplatform.exception.ResourceNotFoundException;
import com.blogplatform.model.*;
import com.blogplatform.repository.*;
import com.blogplatform.util.SecurityUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Tests")
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock PostRepository     postRepository;
    @Mock UserService        userService;
    @Mock LikeService        likeService;
    @Mock SecurityUtil       securityUtil;

    @InjectMocks CommentService commentService;

    private User author;
    private Post publishedPost;
    private Comment existingComment;

    @BeforeEach
    void setUp() {
        author = User.builder().id(1L).username("commenter")
                .firstName("Com").lastName("Menter")
                .role(User.Role.ROLE_USER).enabled(true).build();

        publishedPost = Post.builder()
                .id(10L).title("Post").slug("post")
                .status(Post.Status.PUBLISHED).author(author)
                .tags(List.of()).viewCount(0L)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        existingComment = Comment.builder()
                .id(100L).content("Original comment.")
                .post(publishedPost).author(author)
                .approved(true).replies(List.of())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    // ---- createComment ----

    @Test
    @DisplayName("createComment: success on published post")
    void createComment_success() {
        CommentDto.CreateRequest req = new CommentDto.CreateRequest();
        req.setContent("Great post!");

        when(securityUtil.getCurrentUserOrThrow()).thenReturn(author);
        when(postRepository.findById(10L)).thenReturn(Optional.of(publishedPost));
        when(commentRepository.save(any())).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c = Comment.builder().id(1L).content(c.getContent())
                    .post(publishedPost).author(author).approved(true)
                    .replies(List.of()).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            return c;
        });
        when(userService.mapToSummary(any())).thenReturn(null);
        when(likeService.getLikeCount(any(), any())).thenReturn(0L);
        when(likeService.getDislikeCount(any(), any())).thenReturn(0L);

        CommentDto.Response result = commentService.createComment(10L, req);

        assertThat(result.getContent()).isEqualTo("Great post!");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("createComment: throws BadRequestException on unpublished post")
    void createComment_unpublishedPost_throws() {
        Post draft = Post.builder().id(5L).status(Post.Status.DRAFT).build();
        when(securityUtil.getCurrentUserOrThrow()).thenReturn(author);
        when(postRepository.findById(5L)).thenReturn(Optional.of(draft));

        CommentDto.CreateRequest req = new CommentDto.CreateRequest();
        req.setContent("Hello");

        assertThatThrownBy(() -> commentService.createComment(5L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("unpublished");
    }

    // ---- updateComment ----

    @Test
    @DisplayName("updateComment: owner can update their comment")
    void updateComment_ownerCanUpdate() {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(existingComment));
        when(securityUtil.getCurrentUserOrThrow()).thenReturn(author);
        when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userService.mapToSummary(any())).thenReturn(null);
        when(likeService.getLikeCount(any(), any())).thenReturn(0L);
        when(likeService.getDislikeCount(any(), any())).thenReturn(0L);

        CommentDto.UpdateRequest req = new CommentDto.UpdateRequest();
        req.setContent("Updated content.");

        CommentDto.Response result = commentService.updateComment(100L, req);
        assertThat(result.getContent()).isEqualTo("Updated content.");
    }

    @Test
    @DisplayName("updateComment: non-owner throws AccessDeniedException")
    void updateComment_nonOwner_throws() {
        User other = User.builder().id(99L).role(User.Role.ROLE_USER).build();
        when(commentRepository.findById(100L)).thenReturn(Optional.of(existingComment));
        when(securityUtil.getCurrentUserOrThrow()).thenReturn(other);

        CommentDto.UpdateRequest req = new CommentDto.UpdateRequest();
        req.setContent("Hacked.");

        assertThatThrownBy(() -> commentService.updateComment(100L, req))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---- deleteComment ----

    @Test
    @DisplayName("deleteComment: admin can delete any comment")
    void deleteComment_adminCanDelete() {
        User admin = User.builder().id(999L).role(User.Role.ROLE_ADMIN).build();
        when(commentRepository.findById(100L)).thenReturn(Optional.of(existingComment));
        when(securityUtil.getCurrentUserOrThrow()).thenReturn(admin);

        assertThatNoException().isThrownBy(() -> commentService.deleteComment(100L));
        verify(commentRepository).delete(existingComment);
    }

    // ---- approveComment ----

    @Test
    @DisplayName("approveComment: sets approved=true")
    void approveComment_setsApproved() {
        existingComment.setApproved(false);
        when(commentRepository.findById(100L)).thenReturn(Optional.of(existingComment));
        when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userService.mapToSummary(any())).thenReturn(null);
        when(likeService.getLikeCount(any(), any())).thenReturn(0L);
        when(likeService.getDislikeCount(any(), any())).thenReturn(0L);

        CommentDto.Response result = commentService.approveComment(100L);

        assertThat(existingComment.isApproved()).isTrue();
    }
}
