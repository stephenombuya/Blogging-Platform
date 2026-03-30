package com.blogplatform.dto;

import com.blogplatform.model.Post;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PostDto {

    @Getter @Setter
    public static class CreateRequest {
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 255, message = "Title must be 5-255 characters")
        private String title;

        @NotBlank(message = "Content is required")
        @Size(min = 10, message = "Content must be at least 10 characters")
        private String content;

        @Size(max = 500)
        private String excerpt;

        @Size(max = 500)
        private String coverImageUrl;

        private Post.Status status = Post.Status.DRAFT;

        private Long categoryId;

        private List<String> tags;
    }

    @Getter @Setter
    public static class UpdateRequest {
        @Size(min = 5, max = 255)
        private String title;

        @Size(min = 10)
        private String content;

        @Size(max = 500)
        private String excerpt;

        @Size(max = 500)
        private String coverImageUrl;

        private Post.Status status;

        private Long categoryId;

        private List<String> tags;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Summary {
        private Long id;
        private String title;
        private String slug;
        private String excerpt;
        private String coverImageUrl;
        private Post.Status status;
        private UserDto.Summary author;
        private CategoryDto.Summary category;
        private List<TagDto> tags;
        private Long viewCount;
        private Long likeCount;
        private Long dislikeCount;
        private Long commentCount;
        private LocalDateTime publishedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Detail extends Summary {
        private String content;
        private Boolean likedByCurrentUser;
        private Boolean dislikedByCurrentUser;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class AdminSummary extends Summary {
        private Boolean flagged;
    }
}
