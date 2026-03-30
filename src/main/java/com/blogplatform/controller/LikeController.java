package com.blogplatform.controller;

import com.blogplatform.dto.*;
import com.blogplatform.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reactions", description = "Like/dislike posts and comments")
public class LikeController {

    private final LikeService likeService;

    // ---- Posts ----

    @PostMapping("/posts/{postId}/reactions")
    @Operation(summary = "Like or dislike a post", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<LikeDto.Response>> reactToPost(
            @PathVariable Long postId,
            @Valid @RequestBody LikeDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success(likeService.reactToPost(postId, request)));
    }

    @DeleteMapping("/posts/{postId}/reactions")
    @Operation(summary = "Remove your reaction from a post", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<LikeDto.Response>> removePostReaction(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success("Reaction removed", likeService.removeReactionFromPost(postId)));
    }

    // ---- Comments ----

    @PostMapping("/comments/{commentId}/reactions")
    @Operation(summary = "Like or dislike a comment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<LikeDto.Response>> reactToComment(
            @PathVariable Long commentId,
            @Valid @RequestBody LikeDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success(likeService.reactToComment(commentId, request)));
    }

    @DeleteMapping("/comments/{commentId}/reactions")
    @Operation(summary = "Remove your reaction from a comment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<LikeDto.Response>> removeCommentReaction(@PathVariable Long commentId) {
        return ResponseEntity.ok(ApiResponse.success("Reaction removed", likeService.removeReactionFromComment(commentId)));
    }
}
