package com.blogplatform.controller;

import com.blogplatform.dto.*;
import com.blogplatform.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Blog post CRUD, search and filtering")
public class PostController {

    private final PostService postService;

    @GetMapping
    @Operation(summary = "List all published posts (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<PostDto.Summary>>> getPosts(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(defaultValue = "latest") String sort) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPublishedPosts(page, size, sort)));
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search across published posts")
    public ResponseEntity<ApiResponse<PagedResponse<PostDto.Summary>>> searchPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(postService.searchPosts(query, page, size)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a single post by slug (increments view count)")
    public ResponseEntity<ApiResponse<PostDto.Detail>> getPost(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPostBySlug(slug)));
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "Get a post by ID")
    public ResponseEntity<ApiResponse<PostDto.Detail>> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPostById(id)));
    }

    @GetMapping("/category/{categorySlug}")
    @Operation(summary = "List published posts in a category")
    public ResponseEntity<ApiResponse<PagedResponse<PostDto.Summary>>> getByCategory(
            @PathVariable String categorySlug,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPostsByCategory(categorySlug, page, size)));
    }

    @GetMapping("/tag/{tagSlug}")
    @Operation(summary = "List published posts with a tag")
    public ResponseEntity<ApiResponse<PagedResponse<PostDto.Summary>>> getByTag(
            @PathVariable String tagSlug,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPostsByTag(tagSlug, page, size)));
    }

    @GetMapping("/user/{username}")
    @Operation(summary = "List posts by a user (published only for non-owners)")
    public ResponseEntity<ApiResponse<PagedResponse<PostDto.Summary>>> getByAuthor(
            @PathVariable String username,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPostsByAuthor(username, page, size)));
    }

    @PostMapping
    @Operation(summary = "Create a new post (authenticated users)")
    public ResponseEntity<ApiResponse<PostDto.Detail>> createPost(
            @Valid @RequestBody PostDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created successfully", postService.createPost(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a post (author or admin)")
    public ResponseEntity<ApiResponse<PostDto.Detail>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Post updated", postService.updatePost(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a post (author or admin)")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully"));
    }

    @PatchMapping("/{id}/publish")
    @Operation(summary = "Publish a draft post")
    public ResponseEntity<ApiResponse<PostDto.Detail>> publishPost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Post published", postService.publishPost(id)));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive a post")
    public ResponseEntity<ApiResponse<PostDto.Detail>> archivePost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Post archived", postService.archivePost(id)));
    }
}
