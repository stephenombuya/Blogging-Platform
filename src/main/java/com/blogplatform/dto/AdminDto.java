package com.blogplatform.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AdminDto {

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class DashboardStats {
        private long totalUsers;
        private long totalPosts;
        private long totalComments;
        private long totalCategories;
        private long publishedPosts;
        private long draftPosts;
        private long newUsersThisMonth;
        private long newPostsThisMonth;
        private List<PostDto.Summary> recentPosts;
        private List<UserDto.Profile> recentUsers;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class BulkActionRequest {
        private List<Long> ids;
        private String action; // e.g. "delete", "approve", "publish", "archive"
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class BulkActionResponse {
        private int affected;
        private String message;
    }
}
