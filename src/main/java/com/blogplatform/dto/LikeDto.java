package com.blogplatform.dto;

import com.blogplatform.model.Like;
import jakarta.validation.constraints.*;
import lombok.*;

public class LikeDto {

    @Getter @Setter
    public static class Request {
        @NotNull(message = "Reaction is required")
        private Like.Reaction reaction; // LIKE or DISLIKE
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Response {
        private Long targetId;
        private Like.TargetType targetType;
        private Long likeCount;
        private Long dislikeCount;
        private Like.Reaction userReaction; // null if no reaction
    }
}
