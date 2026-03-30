package com.blogplatform.service;

import com.blogplatform.dto.LikeDto;
import com.blogplatform.exception.*;
import com.blogplatform.model.*;
import com.blogplatform.repository.*;
import com.blogplatform.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final SecurityUtil securityUtil;

    public LikeDto.Response reactToPost(Long postId, LikeDto.Request request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        if (post.getStatus() != Post.Status.PUBLISHED) {
            throw new BadRequestException("Cannot react to an unpublished post");
        }
        return react(securityUtil.getCurrentUserOrThrow(), Like.TargetType.POST, postId, request.getReaction());
    }

    public LikeDto.Response removeReactionFromPost(Long postId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        return removeReaction(securityUtil.getCurrentUserOrThrow(), Like.TargetType.POST, postId);
    }

    public LikeDto.Response reactToComment(Long commentId, LikeDto.Request request) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        return react(securityUtil.getCurrentUserOrThrow(), Like.TargetType.COMMENT, commentId, request.getReaction());
    }

    public LikeDto.Response removeReactionFromComment(Long commentId) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        return removeReaction(securityUtil.getCurrentUserOrThrow(), Like.TargetType.COMMENT, commentId);
    }

    private LikeDto.Response react(User user, Like.TargetType type, Long targetId, Like.Reaction reaction) {
        likeRepository.findByUserAndTargetTypeAndTargetId(user, type, targetId)
                .ifPresentOrElse(
                        existing -> { existing.setReaction(reaction); likeRepository.save(existing); },
                        () -> likeRepository.save(Like.builder()
                                .user(user).targetType(type).targetId(targetId).reaction(reaction).build())
                );
        return buildResponse(user, type, targetId);
    }

    private LikeDto.Response removeReaction(User user, Like.TargetType type, Long targetId) {
        likeRepository.deleteByUserAndTargetTypeAndTargetId(user, type, targetId);
        return buildResponse(user, type, targetId);
    }

    private LikeDto.Response buildResponse(User user, Like.TargetType type, Long targetId) {
        long likes    = likeRepository.countByTargetTypeAndTargetIdAndReaction(type, targetId, Like.Reaction.LIKE);
        long dislikes = likeRepository.countByTargetTypeAndTargetIdAndReaction(type, targetId, Like.Reaction.DISLIKE);
        Like.Reaction userReaction = likeRepository.findReactionByUserAndTarget(user, type, targetId).orElse(null);
        return LikeDto.Response.builder()
                .targetId(targetId).targetType(type)
                .likeCount(likes).dislikeCount(dislikes)
                .userReaction(userReaction)
                .build();
    }

    @Transactional(readOnly = true)
    public long getLikeCount(Like.TargetType type, Long targetId) {
        return likeRepository.countByTargetTypeAndTargetIdAndReaction(type, targetId, Like.Reaction.LIKE);
    }

    @Transactional(readOnly = true)
    public long getDislikeCount(Like.TargetType type, Long targetId) {
        return likeRepository.countByTargetTypeAndTargetIdAndReaction(type, targetId, Like.Reaction.DISLIKE);
    }

    @Transactional(readOnly = true)
    public Like.Reaction getUserReaction(User user, Like.TargetType type, Long targetId) {
        if (user == null) return null;
        return likeRepository.findReactionByUserAndTarget(user, type, targetId).orElse(null);
    }
}
