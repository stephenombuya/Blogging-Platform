package com.blogplatform.dto;

import com.blogplatform.model.Like;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class TagDto {
    private Long id;
    private String name;
    private String slug;
    private Long postCount;
}
