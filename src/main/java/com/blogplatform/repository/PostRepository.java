package com.blogplatform.repository;

import com.blogplatform.model.Post;
import com.blogplatform.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Optional<Post> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Page<Post> findByStatus(Post.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Page<Post> findByAuthorAndStatus(User author, Post.Status status, Pageable pageable);

    Page<Post> findByAuthor(User author, Pageable pageable);

    Page<Post> findByCategoryIdAndStatus(Long categoryId, Post.Status status, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.slug = :tagSlug AND p.status = 'PUBLISHED'")
    Page<Post> findByTagSlugAndStatusPublished(@Param("tagSlug") String tagSlug, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' AND (" +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.excerpt) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Post> searchPublished(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Post> searchAll(@Param("query") String query, Pageable pageable);

    long countByStatus(Post.Status status);

    long countByAuthor(User author);

    long countByCreatedAtAfter(LocalDateTime date);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' ORDER BY p.viewCount DESC")
    Page<Post> findMostViewed(Pageable pageable);

    @Query("SELECT p FROM Post p LEFT JOIN Like l ON l.targetId = p.id AND l.targetType = 'POST' AND l.reaction = 'LIKE' " +
           "WHERE p.status = 'PUBLISHED' GROUP BY p.id ORDER BY COUNT(l.id) DESC")
    Page<Post> findMostLiked(Pageable pageable);
}
