package com.blogplatform.service;

import com.blogplatform.dto.AdminDto;
import com.blogplatform.dto.PostDto;
import com.blogplatform.dto.UserDto;
import com.blogplatform.model.Post;
import com.blogplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository    userRepository;
    private final PostRepository    postRepository;
    private final CommentRepository commentRepository;
    private final CategoryRepository categoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PostService    postService;
    private final UserService    userService;

    public AdminDto.DashboardStats getDashboardStats() {
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);

        List<PostDto.Summary> recentPosts = postRepository
                .findAll(PageRequest.of(0, 5, Sort.by("createdAt").descending()))
                .stream()
                .map(p -> postService.mapToSummary(p, null))
                .toList();

        List<UserDto.Profile> recentUsers = userRepository
                .findAll(PageRequest.of(0, 5, Sort.by("createdAt").descending()))
                .stream()
                .map(userService::mapToProfile)
                .toList();

        return AdminDto.DashboardStats.builder()
                .totalUsers(userRepository.count())
                .totalPosts(postRepository.count())
                .totalComments(commentRepository.countAll())
                .totalCategories(categoryRepository.count())
                .publishedPosts(postRepository.countByStatus(Post.Status.PUBLISHED))
                .draftPosts(postRepository.countByStatus(Post.Status.DRAFT))
                .newUsersThisMonth(userRepository.countByCreatedAtAfter(monthAgo))
                .newPostsThisMonth(postRepository.countByCreatedAtAfter(monthAgo))
                .recentPosts(recentPosts)
                .recentUsers(recentUsers)
                .build();
    }

    /** Scheduled cleanup of expired tokens — runs every day at 2 AM */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int rt  = refreshTokenRepository.deleteExpiredTokens(now);
        int prt = passwordResetTokenRepository.deleteExpiredAndUsedTokens(now);
        int evt = emailVerificationTokenRepository.deleteExpiredAndUsedTokens(now);
    }
}
