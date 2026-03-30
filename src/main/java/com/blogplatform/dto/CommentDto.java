package com.blogplatform.dto;

import com.blogplatform.model.Like;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

// ===== Comment DTO =====
public class CommentDto {

    @Getter @Setter
    public static class CreateRequest {
        @NotBlank(message = "Comment content is required")
        @Size(min = 1, max = 2000, message = "Comment must be 1-2000 characters")
        private String content;

        private Long parentId;
    }

    @Getter @Setter
    public static class UpdateRequest {
        @NotBlank(message = "Content is required")
        @Size(min = 1, max = 2000)
        private String content;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Response {
        private Long id;
        private String content;
        private UserDto.Summary author;
        private Long postId;
        private Long parentId;
        private List<Response> replies;
        private boolean approved;
        private Long likeCount;
        private Long dislikeCount;
        private Boolean likedByCurrentUser;
        private Boolean dislikedByCurrentUser;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
