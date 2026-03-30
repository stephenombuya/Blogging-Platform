package com.blogplatform.dto;

import com.blogplatform.model.User;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class AuthDto {

    // ---- Request DTOs ----

    @Getter @Setter
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, digits and underscores")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100)
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                 message = "Password must contain uppercase, lowercase, digit and special character")
        private String password;

        @NotBlank(message = "First name is required")
        @Size(max = 50)
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 50)
        private String lastName;
    }

    @Getter @Setter
    public static class LoginRequest {
        @NotBlank(message = "Email or username is required")
        private String usernameOrEmail;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Getter @Setter
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Getter @Setter
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
    }

    @Getter @Setter
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                 message = "Password must contain uppercase, lowercase, digit and special character")
        private String newPassword;
    }

    @Getter @Setter
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                 message = "Password must contain uppercase, lowercase, digit and special character")
        private String newPassword;
    }

    // ---- Response DTOs ----

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn;
        private UserDto.Summary user;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String tokenType = "Bearer";
        private Long expiresIn;
    }
}
