package com.blogplatform.service;

import com.blogplatform.dto.*;
import com.blogplatform.exception.*;
import com.blogplatform.model.*;
import com.blogplatform.repository.*;
import com.blogplatform.util.SecurityUtil;
import com.blogplatform.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository     postRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository  commentRepository;
    private final UserService        userService;
    private final TagService         tagService;
    private final LikeService        likeService;
    private final CategoryService    categoryService;
    private final SecurityUtil       securityUtil;
    private final SlugUtil           slugUtil;

    /** Public feed: only PUBLISHED posts */
    public PagedResponse<PostDto.Summary> getPublishedPosts(int page, int size, String sort) {
        Sort s = resolveSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);
        return PagedResponse.of(postRepository.findByStatus(Post.Status.PUBLISHED, pageable)
                .map(p -> mapToSummary(p, currentUser())));
    }

    /** Admin / moderator: all posts */
    public PagedResponse<PostDto.Summary> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PagedResponse.of(postRepository.findAll(pageable)
                .map(p -> mapToSummary(p, currentUser())));
    }

    public PagedResponse<PostDto.Summary> getPostsByCategory(String categorySlug, int page, int size) {
        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", categorySlug));
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return PagedResponse.of(
                postRepository.findByCategoryIdAndStatus(category.getId(), Post.Status.PUBLISHED, pageable)
                        .map(p -> mapToSummary(p, currentUser())));
    }

    public PagedResponse<PostDto.Summary> getPostsByTag(String tagSlug, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return PagedResponse.of(
                postRepository.findByTagSlugAndStatusPublished(tagSlug, pageable)
                        .map(p -> mapToSummary(p, currentUser())));
    }

    public PagedResponse<PostDto.Summary> getPostsByAuthor(String username, int page, int size) {
        User author = findUserByUsername(username);
        User viewer = currentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Show all statuses only to the author themselves or admins
        boolean showAll = viewer != null && (viewer.getId().equals(author.getId())
                || viewer.getRole() == User.Role.ROLE_ADMIN);

        return PagedResponse.of(
                showAll ? postRepository.findByAuthor(author, pageable).map(p -> mapToSummary(p, viewer))
                        : postRepository.findByAuthorAndStatus(author, Post.Status.PUBLISHED, pageable)
                                .map(p -> mapToSummary(p, viewer)));
    }

    public PagedResponse<PostDto.Summary> searchPosts(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        User viewer = currentUser();
        boolean isAdmin = viewer != null && viewer.getRole() == User.Role.ROLE_ADMIN;
        return PagedResponse.of(
                isAdmin ? postRepository.searchAll(query, pageable).map(p -> mapToSummary(p, viewer))
                        : postRepository.searchPublished(query, pageable).map(p -> mapToSummary(p, viewer)));
    }

    @Transactional
    public PostDto.Detail getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "slug", slug));

        User viewer = currentUser();
        boolean isOwnerOrAdmin = viewer != null && (viewer.getId().equals(post.getAuthor().getId())
                || viewer.getRole() == User.Role.ROLE_ADMIN);

        if (post.getStatus() != Post.Status.PUBLISHED && !isOwnerOrAdmin) {
            throw new ResourceNotFoundException("Post", "slug", slug);
        }

        // Increment view count asynchronously
        postRepository.incrementViewCount(post.getId());

        return mapToDetail(post, viewer);
    }

    public PostDto.Detail getPostById(Long id) {
        Post post = findById(id);
        User viewer = currentUser();
        boolean isOwnerOrAdmin = viewer != null && (viewer.getId().equals(post.getAuthor().getId())
                || viewer.getRole() == User.Role.ROLE_ADMIN);
        if (post.getStatus() != Post.Status.PUBLISHED && !isOwnerOrAdmin) {
            throw new ResourceNotFoundException("Post", "id", id);
        }
        return mapToDetail(post, viewer);
    }

    @Transactional
    public PostDto.Detail createPost(PostDto.CreateRequest request) {
        User author = securityUtil.getCurrentUserOrThrow();
        String slug = slugUtil.makeUnique(slugUtil.toSlug(request.getTitle()), postRepository::existsBySlug);

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        Post post = Post.builder()
                .title(request.getTitle())
                .slug(slug)
                .content(request.getContent())
                .excerpt(request.getExcerpt())
                .coverImageUrl(request.getCoverImageUrl())
                .status(request.getStatus() != null ? request.getStatus() : Post.Status.DRAFT)
                .author(author)
                .category(category)
                .tags(tagService.findOrCreateAll(request.getTags()))
                .build();

        if (post.getStatus() == Post.Status.PUBLISHED) {
            post.setPublishedAt(LocalDateTime.now());
        }

        post = postRepository.save(post);
        log.info("Post created: id={}, author={}", post.getId(), author.getUsername());
        return mapToDetail(post, author);
    }

    @Transactional
    public PostDto.Detail updatePost(Long id, PostDto.UpdateRequest request) {
        Post post = findById(id);
        User viewer = securityUtil.getCurrentUserOrThrow();

        if (!viewer.getId().equals(post.getAuthor().getId()) && viewer.getRole() != User.Role.ROLE_ADMIN) {
            throw new AccessDeniedException("You don't have permission to edit this post");
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
            // Only re-slug if title changed and new slug would differ
            String newSlug = slugUtil.toSlug(request.getTitle());
            if (!post.getSlug().startsWith(newSlug)) {
                post.setSlug(slugUtil.makeUnique(newSlug,
                        s -> !s.equals(post.getSlug()) && postRepository.existsBySlug(s)));
            }
        }
        if (request.getContent()       != null) post.setContent(request.getContent());
        if (request.getExcerpt()       != null) post.setExcerpt(request.getExcerpt());
        if (request.getCoverImageUrl() != null) post.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getCategoryId()    != null) {
            post.setCategory(categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId())));
        }
        if (request.getTags() != null) {
            post.setTags(tagService.findOrCreateAll(request.getTags()));
        }
        if (request.getStatus() != null) {
            Post.Status oldStatus = post.getStatus();
            post.setStatus(request.getStatus());
            if (request.getStatus() == Post.Status.PUBLISHED && oldStatus != Post.Status.PUBLISHED) {
                post.setPublishedAt(LocalDateTime.now());
            }
        }

        log.info("Post updated: id={}", id);
        return mapToDetail(postRepository.save(post), viewer);
    }

    @Transactional
    public void deletePost(Long id) {
        Post post = findById(id);
        User viewer = securityUtil.getCurrentUserOrThrow();
        if (!viewer.getId().equals(post.getAuthor().getId()) && viewer.getRole() != User.Role.ROLE_ADMIN) {
            throw new AccessDeniedException("You don't have permission to delete this post");
        }
        postRepository.delete(post);
        log.info("Post deleted: id={}", id);
    }

    @Transactional
    public PostDto.Detail publishPost(Long id) {
        Post post = findById(id);
        User viewer = securityUtil.getCurrentUserOrThrow();
        if (!viewer.getId().equals(post.getAuthor().getId()) && viewer.getRole() != User.Role.ROLE_ADMIN) {
            throw new AccessDeniedException("You don't have permission to publish this post");
        }
        post.setStatus(Post.Status.PUBLISHED);
        post.setPublishedAt(LocalDateTime.now());
        return mapToDetail(postRepository.save(post), viewer);
    }

    @Transactional
    public PostDto.Detail archivePost(Long id) {
        Post post = findById(id);
        User viewer = securityUtil.getCurrentUserOrThrow();
        if (!viewer.getId().equals(post.getAuthor().getId()) && viewer.getRole() != User.Role.ROLE_ADMIN) {
            throw new AccessDeniedException("You don't have permission to archive this post");
        }
        post.setStatus(Post.Status.ARCHIVED);
        return mapToDetail(postRepository.save(post), viewer);
    }

    // ---- Mapping helpers ----

    public PostDto.Summary mapToSummary(Post post, User viewer) {
        return PostDto.Summary.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .excerpt(post.getExcerpt())
                .coverImageUrl(post.getCoverImageUrl())
                .status(post.getStatus())
                .author(userService.mapToSummary(post.getAuthor()))
                .category(categoryService.mapToSummary(post.getCategory()))
                .tags(post.getTags().stream().map(tagService::mapToDto).toList())
                .viewCount(post.getViewCount())
                .likeCount(likeService.getLikeCount(Like.TargetType.POST, post.getId()))
                .dislikeCount(likeService.getDislikeCount(Like.TargetType.POST, post.getId()))
                .commentCount(commentRepository.countByPostAndApprovedTrue(post))
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private PostDto.Detail mapToDetail(Post post, User viewer) {
        PostDto.Detail detail = new PostDto.Detail();
        detail.setId(post.getId());
        detail.setTitle(post.getTitle());
        detail.setSlug(post.getSlug());
        detail.setContent(post.getContent());
        detail.setExcerpt(post.getExcerpt());
        detail.setCoverImageUrl(post.getCoverImageUrl());
        detail.setStatus(post.getStatus());
        detail.setAuthor(userService.mapToSummary(post.getAuthor()));
        detail.setCategory(categoryService.mapToSummary(post.getCategory()));
        detail.setTags(post.getTags().stream().map(tagService::mapToDto).toList());
        detail.setViewCount(post.getViewCount());
        detail.setLikeCount(likeService.getLikeCount(Like.TargetType.POST, post.getId()));
        detail.setDislikeCount(likeService.getDislikeCount(Like.TargetType.POST, post.getId()));
        detail.setCommentCount(commentRepository.countByPostAndApprovedTrue(post));
        detail.setPublishedAt(post.getPublishedAt());
        detail.setCreatedAt(post.getCreatedAt());
        detail.setUpdatedAt(post.getUpdatedAt());
        if (viewer != null) {
            Like.Reaction r = likeService.getUserReaction(viewer, Like.TargetType.POST, post.getId());
            detail.setLikedByCurrentUser(r == Like.Reaction.LIKE);
            detail.setDislikedByCurrentUser(r == Like.Reaction.DISLIKE);
        }
        return detail;
    }

    private Sort resolveSort(String sort) {
        if (sort == null) return Sort.by("publishedAt").descending();
        return switch (sort) {
            case "oldest"   -> Sort.by("publishedAt").ascending();
            case "popular"  -> Sort.by("viewCount").descending();
            case "title"    -> Sort.by("title").ascending();
            default         -> Sort.by("publishedAt").descending();
        };
    }

    private Post findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
    }

    private User findUserByUsername(String username) {
        // Inline lookup to avoid circular dependency
        return postRepository.findAll().stream()
                .map(Post::getAuthor)
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    private User currentUser() {
        return securityUtil.getCurrentUser().orElse(null);
    }
}
