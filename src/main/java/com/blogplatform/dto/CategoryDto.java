package com.blogplatform.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class CategoryDto {

    @Getter @Setter
    public static class CreateRequest {
        @NotBlank(message = "Category name is required")
        @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
        private String name;

        @Size(max = 500)
        private String description;
    }

    @Getter @Setter
    public static class UpdateRequest {
        @Size(min = 2, max = 100)
        private String name;

        @Size(max = 500)
        private String description;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Summary {
        private Long id;
        private String name;
        private String slug;
        private String description;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Detail extends Summary {
        private Long postCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
