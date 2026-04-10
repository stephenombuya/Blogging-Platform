package com.blogplatform.repository;

import com.blogplatform.model.Comment;
import com.blogplatform.model.Post;
import com.blogplatform.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Top-level approved comments for a post (no parent)
    @EntityGraph(attributePaths = {"author", "replies", "replies.author"})
    Page<Comment> findByPostAndParentIsNullAndApprovedTrue(
        Post post, Pageable pageable);

    // All top-level comments (for admin)
    Page<Comment> findByPostAndParentIsNull(Post post, Pageable pageable);

    // Replies to a comment
    Page<Comment> findByParentAndApprovedTrue(Comment parent, Pageable pageable);

    // All comments by a user
    Page<Comment> findByAuthor(User author, Pageable pageable);

    // Pending moderation
    Page<Comment> findByApprovedFalse(Pageable pageable);

    long countByPostAndApprovedTrue(Post post);

    long countByPost(Post post);

    long countByAuthor(User author);

    @Query("SELECT COUNT(c) FROM Comment c")
    long countAll();

    @Query("SELECT c FROM Comment c WHERE c.approved = false ORDER BY c.createdAt DESC")
    Page<Comment> findPendingModeration(Pageable pageable);
}
