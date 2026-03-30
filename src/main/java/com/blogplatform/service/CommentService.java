package com.blogplatform.service;

import com.blogplatform.dto.CommentDto;
import com.blogplatform.dto.PagedResponse;
import com.blogplatform.exception.*;
import com.blogplatform.model.*;
import com.blogplatform.repository.*;
import com.blogplatform.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository    postRepository;
    private final UserService       userService;
    private final LikeService       likeService;
    private final SecurityUtil      securityUtil;

    public PagedResponse<CommentDto.Response> getCommentsForPost(Long postId, int page, int size) {
        Post post = findPost(postId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<Comment> comments = commentRepository.findByPostAndParentIsNullAndApprovedTrue(post, pageable);
        User viewer = securityUtil.getCurrentUser().orElse(null);
        return PagedResponse.of(comments.map(c -> mapToResponse(c, viewer, true)));
    }

    public PagedResponse<CommentDto.Response> getAllCommentsForPost(Long postId, int page, int size) {
        Post post = findPost(postId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<Comment> comments = commentRepository.findByPostAndParentIsNull(post, pageable);
        User viewer = securityUtil.getCurrentUser().orElse(null);
        return PagedResponse.of(comments.map(c -> mapToResponse(c, viewer, true)));
    }

    @Transactional
    public CommentDto.Response createComment(Long postId, CommentDto.CreateRequest request) {
        User author = securityUtil.getCurrentUserOrThrow();
        Post post   = findPost(postId);

        if (post.getStatus() != Post.Status.PUBLISHED) {
            throw new BadRequestException("Cannot comment on an unpublished post");
        }

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getParentId()));
            if (!parent.getPost().getId().equals(postId)) {
                throw new BadRequestException("Parent comment does not belong to this post");
            }
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .post(post)
                .author(author)
                .parent(parent)
                .approved(true)
                .build();

        comment = commentRepository.save(comment);
        log.info("Comment created: id={} on post={}", comment.getId(), postId);
        return mapToResponse(comment, author, false);
    }

    @Transactional
    public CommentDto.Response updateComment(Long commentId, CommentDto.UpdateRequest request) {
        Comment comment = findById(commentId);
        User viewer = securityUtil.getCurrentUserOrThrow();

        if (!viewer.getId().equals(comment.getAuthor().getId()) && viewer.getRole() != User.Role.ROLE_ADMIN) {
            throw new AccessDeniedException("You don't have permission to edit this comment");
        }

        comment.setContent(request.getContent());
        return mapToResponse(commentRepository.save(comment), viewer, false);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = findById(commentId);
        User viewer = securityUtil.getCurrentUserOrThrow();

        if (!viewer.getId().equals(comment.getAuthor().getId()) && viewer.getRole() != User.Role.ROLE_ADMIN) {
            throw new AccessDeniedException("You don't have permission to delete this comment");
        }
        commentRepository.delete(comment);
        log.info("Comment deleted: id={}", commentId);
    }

    @Transactional
    public CommentDto.Response approveComment(Long commentId) {
        Comment comment = findById(commentId);
        comment.setApproved(true);
        return mapToResponse(commentRepository.save(comment), null, false);
    }

    @Transactional
    public CommentDto.Response rejectComment(Long commentId) {
        Comment comment = findById(commentId);
        comment.setApproved(false);
        return mapToResponse(commentRepository.save(comment), null, false);
    }

    public PagedResponse<CommentDto.Response> getPendingComments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return PagedResponse.of(commentRepository.findPendingModeration(pageable)
                .map(c -> mapToResponse(c, null, false)));
    }

    // ---- Mapping ----

    private CommentDto.Response mapToResponse(Comment comment, User viewer, boolean includeReplies) {
        List<CommentDto.Response> replies = null;
        if (includeReplies && comment.getReplies() != null) {
            replies = comment.getReplies().stream()
                    .filter(Comment::isApproved)
                    .map(r -> mapToResponse(r, viewer, false))
                    .toList();
        }

        Like.Reaction userReaction = viewer != null
                ? likeService.getUserReaction(viewer, Like.TargetType.COMMENT, comment.getId())
                : null;

        return CommentDto.Response.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(userService.mapToSummary(comment.getAuthor()))
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replies(replies)
                .approved(comment.isApproved())
                .likeCount(likeService.getLikeCount(Like.TargetType.COMMENT, comment.getId()))
                .dislikeCount(likeService.getDislikeCount(Like.TargetType.COMMENT, comment.getId()))
                .likedByCurrentUser(userReaction == Like.Reaction.LIKE)
                .dislikedByCurrentUser(userReaction == Like.Reaction.DISLIKE)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
    }

    private Comment findById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
    }
}
