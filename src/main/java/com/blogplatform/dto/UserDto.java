package com.blogplatform.dto;

import com.blogplatform.model.User;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class UserDto {

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Summary {
        private Long id;
        private String username;
        private String firstName;
        private String lastName;
        private String fullName;
        private String avatarUrl;
        private User.Role role;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Profile {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String fullName;
        private String bio;
        private String avatarUrl;
        private User.Role role;
        private boolean enabled;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;
        private Long postCount;
    }

    @Getter @Setter
    public static class UpdateRequest {
        @Size(max = 50)
        private String firstName;

        @Size(max = 50)
        private String lastName;

        @Size(max = 500)
        private String bio;

        @Size(max = 500)
        private String avatarUrl;
    }

    @Getter @Setter
    public static class AdminUpdateRequest {
        @Size(max = 50)
        private String firstName;

        @Size(max = 50)
        private String lastName;

        @Size(max = 500)
        private String bio;

        private User.Role role;
        private Boolean enabled;
        private Boolean locked;
    }
}
