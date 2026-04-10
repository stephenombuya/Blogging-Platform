package com.blogplatform.controller;

import com.blogplatform.dto.*;
import com.blogplatform.service.AdminService;
import com.blogplatform.service.PostService;
import com.blogplatform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only management endpoints")
public class AdminController {

    private final AdminService adminService;
    private final UserService  userService;
    private final PostService  postService;

    // ---- Dashboard ----

    @GetMapping("/dashboard")
    @Operation(summary = "Get platform statistics dashboard", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AdminDto.DashboardStats>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    // ---- User Management ----

    @GetMapping("/users")
    @Operation(summary = "List all users (paginated, searchable)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PagedResponse<UserDto.Profile>>> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String search) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(page, size, search)));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get any user by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserDto.Profile>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Admin update of any user (role, enabled, locked, etc.)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserDto.Profile>> adminUpdateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDto.AdminUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated", userService.adminUpdateUser(id, request)));
    }

    @PatchMapping("/users/{id}/enable")
    @Operation(summary = "Enable a user account", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable Long id) {
        userService.toggleUserEnabled(id, true);
        return ResponseEntity.ok(ApiResponse.success("User enabled"));
    }

    @PatchMapping("/users/{id}/disable")
    @Operation(summary = "Disable a user account", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable Long id) {
        userService.toggleUserEnabled(id, false);
        return ResponseEntity.ok(ApiResponse.success("User disabled"));
    }

    @PatchMapping("/users/{id}/lock")
    @Operation(summary = "Lock a user account", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> lockUser(@PathVariable Long id) {
        userService.toggleUserLocked(id, true);
        return ResponseEntity.ok(ApiResponse.success("User locked"));
    }

    @PatchMapping("/users/{id}/unlock")
    @Operation(summary = "Unlock a user account", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> unlockUser(@PathVariable Long id) {
        userService.toggleUserLocked(id, false);
        return ResponseEntity.ok(ApiResponse.success("User unlocked"));
    }

    @PatchMapping("/posts/{id}/restore")
    @Operation(summary = "Restore a soft-deleted post")
    public ResponseEntity<ApiResponse<Void>> restorePost(@PathVariable Long id) {
        postService.restorePost(id);
        return ResponseEntity.ok(ApiResponse.success("Post restored"));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete any user account", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted"));
    }

    // ---- Post Management ----

    @GetMapping("/posts")
    @Operation(summary = "List all posts (all statuses)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PagedResponse<PostDto.Summary>>> getAllPosts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(postService.getAllPosts(page, size)));
    }

    @PatchMapping("/posts/{id}/publish")
    @Operation(summary = "Publish a post", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PostDto.Detail>> publishPost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Post published", postService.publishPost(id)));
    }

    @PatchMapping("/posts/{id}/archive")
    @Operation(summary = "Archive a post", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PostDto.Detail>> archivePost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Post archived", postService.archivePost(id)));
    }

    @DeleteMapping("/posts/{id}")
    @Operation(summary = "Admin delete any post", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success("Post deleted"));
    }
}
