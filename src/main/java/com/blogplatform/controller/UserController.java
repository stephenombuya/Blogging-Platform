package com.blogplatform.controller;

import com.blogplatform.dto.*;
import com.blogplatform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
public class UserController {

    private final UserService userService;

    @GetMapping("/{username}/profile")
    @Operation(summary = "Get public profile by username")
    public ResponseEntity<ApiResponse<UserDto.Profile>> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByUsername(username)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's profile")
    public ResponseEntity<ApiResponse<UserDto.Profile>> getMyProfile(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.blogplatform.model.User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(currentUser.getId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile (owner or admin only)")
    public ResponseEntity<ApiResponse<UserDto.Profile>> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userService.updateProfile(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user account (owner or admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
}
