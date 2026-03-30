package com.blogplatform.controller;

import com.blogplatform.dto.ApiResponse;
import com.blogplatform.dto.AuthDto;
import com.blogplatform.model.User;
import com.blogplatform.service.AuthService;
import com.blogplatform.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, email verification, and password management")
public class AuthController {

    private final AuthService  authService;
    private final SecurityUtil securityUtil;

    @PostMapping("/register")
    @SecurityRequirements  // No auth needed
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful. Please check your email to verify your account.",
                        authService.register(request)));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login and obtain JWT tokens")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Refresh access token using a refresh token")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> refresh(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(request.getRefreshToken())));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/verify-email")
    @SecurityRequirements
    @Operation(summary = "Verify user email address via token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully. You can now log in."));
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(summary = "Request a password reset email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody AuthDto.ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(summary = "Reset password using the token from email")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody AuthDto.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. Please log in."));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password for the authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        User currentUser = securityUtil.getCurrentUserOrThrow();
        authService.changePassword(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully."));
    }
}
