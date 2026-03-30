package com.blogplatform.controller;

import com.blogplatform.dto.*;
import com.blogplatform.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Post tag management")
public class TagController {

    private final TagService tagService;

    @GetMapping("/popular")
    @Operation(summary = "Get most-used tags")
    public ResponseEntity<ApiResponse<List<TagDto>>> getPopularTags(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(tagService.getPopularTags(limit)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search tags by name")
    public ResponseEntity<ApiResponse<List<TagDto>>> searchTags(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(tagService.searchTags(q)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get tag details by slug")
    public ResponseEntity<ApiResponse<TagDto>> getTagBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(tagService.getTagBySlug(slug)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a tag (admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success("Tag deleted"));
    }
}
