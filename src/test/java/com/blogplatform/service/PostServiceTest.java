package com.blogplatform.service;

import com.blogplatform.dto.PostDto;
import com.blogplatform.exception.AccessDeniedException;
import com.blogplatform.exception.ResourceNotFoundException;
import com.blogplatform.model.*;
import com.blogplatform.repository.*;
import com.blogplatform.util.SecurityUtil;
import com.blogplatform.util.SlugUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService Tests")
class PostServiceTest {

    @Mock PostRepository     postRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock CommentRepository  commentRepository;
    @Mock UserService        userService;
    @Mock TagService         tagService;
    @Mock LikeService        likeService;
    @Mock CategoryService    categoryService;
    @Mock SecurityUtil       securityUtil;
    @Mock SlugUtil           slugUtil;

    @InjectMocks PostService postService;

    private User author;
    private Post publishedPost;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id(1L).username("author").email("author@test.com")
                .firstName("Author").lastName("Test")
                .role(User.Role.ROLE_USER).enabled(true).build();

        publishedPost = Post.builder()
                .id(10L).title("Test Post").slug("test-post")
                .content("Full content here.").excerpt("Short excerpt.")
                .status(Post.Status.PUBLISHED)
                .author(author).tags(List.of()).viewCount(0L)
                .publishedAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // ---- getPostBySlug ----

    @Test
    @DisplayName("getPostBySlug: returns post and increments view count")
    void getPostBySlug_found_incrementsViewCount() {
        when(postRepository.findBySlug("test-post")).thenReturn(Optional.of(publishedPost));
        when(securityUtil.getCurrentUser()).thenReturn(Optional.empty());
        when(userService.mapToSummary(any())).thenReturn(null);
        when(categoryService.mapToSummary(any())).thenReturn(null);
        when(tagService.mapToDto(any())).thenReturn(null);
        when(likeService.getLikeCount(any(), any())).thenReturn(0L);
        when(likeService.getDislikeCount(any(), any())).thenReturn(0L);
        when(commentRepository.countByPostAndApprovedTrue(any())).thenReturn(0L);

        PostDto.Detail result = postService.getPostBySlug("test-post");

        assertThat(result.getSlug()).isEqualTo("test-post");
        verify(postRepository).incrementViewCount(10L);
    }

    @Test
    @DisplayName("getPostBySlug: throws ResourceNotFoundException for unknown slug")
    void getPostBySlug_notFound_throws() {
        when(postRepository.findBySlug("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.getPostBySlug("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getPostBySlug: draft hidden from non-owners")
    void getPostBySlug_draft_hiddenFromPublic() {
        Post draft = Post.builder().id(2L).slug("draft-post")
                .status(Post.Status.DRAFT).author(author)
                .tags(List.of()).viewCount(0L)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(postRepository.findBySlug("draft-post")).thenReturn(Optional.of(draft));
        when(securityUtil.getCurrentUser()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostBySlug("draft-post"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- createPost ----

    @Test
    @DisplayName("createPost: success — returns detail with correct author")
    void createPost_success() {
        PostDto.CreateRequest request = new PostDto.CreateRequest();
        request.setTitle("New Post");
        request.setContent("Content body.");
        request.setStatus(Post.Status.DRAFT);

        when(securityUtil.getCurrentUserOrThrow()).thenReturn(author);
        when(slugUtil.toSlug("New Post")).thenReturn("new-post");
        when(slugUtil.makeUnique(eq("new-post"), any())).thenReturn("new-post");
        when(tagService.findOrCreateAll(any())).thenReturn(List.of());
        when(postRepository.save(any())).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p = Post.builder().id(99L).title(p.getTitle()).slug(p.getSlug())
                    .content(p.getContent()).status(p.getStatus())
                    .author(author).tags(List.of()).viewCount(0L)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            return p;
        });
        when(userService.mapToSummary(any())).thenReturn(null);
        when(categoryService.mapToSummary(any())).thenReturn(null);
        when(likeService.getLikeCount(any(), any())).thenReturn(0L);
        when(likeService.getDislikeCount(any(), any())).thenReturn(0L);
        when(commentRepository.countByPostAndApprovedTrue(any())).thenReturn(0L);

        PostDto.Detail result = postService.createPost(request);

        assertThat(result.getTitle()).isEqualTo("New Post");
        assertThat(result.getStatus()).isEqualTo(Post.Status.DRAFT);
        verify(postRepository).save(any(Post.class));
    }

    // ---- updatePost ----

    @Test
    @DisplayName("updatePost: throws AccessDeniedException when not owner and not admin")
    void updatePost_notOwner_throws() {
        User other = User.builder().id(99L).role(User.Role.ROLE_USER).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(publishedPost));
        when(securityUtil.getCurrentUserOrThrow()).thenReturn(other);

        PostDto.UpdateRequest req = new PostDto.UpdateRequest();
        req.setTitle("Hacked Title");

        assertThatThrownBy(() -> postService.updatePost(10L, req))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("updatePost: admin can edit any post")
    void updatePost_adminCanEditAny() {
        User admin = User.builder().id(50L).role(User.Role.ROLE_ADMIN)
                .username("admin").firstName("Admin").lastName("User").build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(publishedPost));
        when(securityUtil.getCurrentUserOrThrow()).thenReturn(admin);
        when(slugUtil.toSlug(any())).thenReturn("updated-title");
        when(postRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userService.mapToSummary(any())).thenReturn(null);
        when(categoryService.mapToSummary(any())).thenReturn(null);
        when(likeService.getLikeCount(any(), any())).thenReturn(0L);
        when(likeService.getDislikeCount(any(), any())).thenReturn(0L);
        when(commentRepository.countByPostAndApprovedTrue(any())).thenReturn(0L);

        PostDto.UpdateRequest req = new PostDto.UpdateRequest();
        req.setTitle("Updated Title");

        assertThatNoException().isThrownBy(() -> postService.updatePost(10L, req));
    }

    // ---- deletePost ----

    @Test
    @DisplayName("deletePost: owner can delete their own post")
    void deletePost_ownerCanDelete() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(publishedPost));
        when(securityUtil.getCurrentUserOrThrow()).thenReturn(author);

        assertThatNoException().isThrownBy(() -> postService.deletePost(10L));
        verify(postRepository).delete(publishedPost);
    }

    @Test
    @DisplayName("deletePost: non-owner throws AccessDeniedException")
    void deletePost_nonOwner_throws() {
        User other = User.builder().id(99L).role(User.Role.ROLE_USER).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(publishedPost));
        when(securityUtil.getCurrentUserOrThrow()).thenReturn(other);

        assertThatThrownBy(() -> postService.deletePost(10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---- publishPost ----

    @Test
    @DisplayName("publishPost: sets status PUBLISHED and publishedAt")
    void publishPost_setsPublishedAt() {
        Post draft = Post.builder().id(5L).slug("draft").status(Post.Status.DRAFT)
                .author(author).tags(List.of()).viewCount(0L)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(postRepository.findById(5L)).thenReturn(Optional.of(draft));
        when(securityUtil.getCurrentUserOrThrow()).thenReturn(author);
        when(postRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userService.mapToSummary(any())).thenReturn(null);
        when(categoryService.mapToSummary(any())).thenReturn(null);
        when(likeService.getLikeCount(any(), any())).thenReturn(0L);
        when(likeService.getDislikeCount(any(), any())).thenReturn(0L);
        when(commentRepository.countByPostAndApprovedTrue(any())).thenReturn(0L);

        PostDto.Detail result = postService.publishPost(5L);

        assertThat(draft.getStatus()).isEqualTo(Post.Status.PUBLISHED);
        assertThat(draft.getPublishedAt()).isNotNull();
    }
}
