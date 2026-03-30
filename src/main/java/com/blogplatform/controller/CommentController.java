package com.blogplatform.controller;

import com.blogplatform.dto.*;
import com.blogplatform.model.User;
import com.blogplatform.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Comment management for posts")
public class CommentController {

    private final CommentService commentService;

    // ---- Public ----

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "Get approved comments for a post (paginated, threaded)")
    public ResponseEntity<ApiResponse<PagedResponse<CommentDto.Response>>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsForPost(postId, page, size)));
    }

    // ---- Authenticated ----

    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "Add a comment (or reply) to a post", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CommentDto.Response>> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentDto.CreateRequest request) {
        CommentDto.Response response = commentService.createComment(postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Comment created", response));
    }

    @PutMapping("/comments/{id}")
    @Operation(summary = "Edit a comment (owner or admin)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CommentDto.Response>> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Comment updated", commentService.updateComment(id, request)));
    }

    @DeleteMapping("/comments/{id}")
    @Operation(summary = "Delete a comment (owner or admin)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted"));
    }

    // ---- Admin / Moderator ----

    @GetMapping("/posts/{postId}/comments/all")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @Operation(summary = "Get ALL comments for a post (including unapproved) — admin/moderator",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PagedResponse<CommentDto.Response>>> getAllComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getAllCommentsForPost(postId, page, size)));
    }

    @GetMapping("/moderator/comments/pending")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @Operation(summary = "Get comments pending moderation", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PagedResponse<CommentDto.Response>>> getPendingComments(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getPendingComments(page, size)));
    }

    @PatchMapping("/moderator/comments/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @Operation(summary = "Approve a comment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CommentDto.Response>> approveComment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Comment approved", commentService.approveComment(id)));
    }

    @PatchMapping("/moderator/comments/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @Operation(summary = "Reject/hide a comment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CommentDto.Response>> rejectComment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Comment rejected", commentService.rejectComment(id)));
    }
}
