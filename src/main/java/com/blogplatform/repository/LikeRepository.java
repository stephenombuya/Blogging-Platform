package com.blogplatform.repository;

import com.blogplatform.model.Like;
import com.blogplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserAndTargetTypeAndTargetId(User user, Like.TargetType targetType, Long targetId);

    long countByTargetTypeAndTargetIdAndReaction(Like.TargetType targetType, Long targetId, Like.Reaction reaction);

    void deleteByUserAndTargetTypeAndTargetId(User user, Like.TargetType targetType, Long targetId);

    boolean existsByUserAndTargetTypeAndTargetId(User user, Like.TargetType targetType, Long targetId);

    @Query("SELECT l.reaction FROM Like l WHERE l.user = :user AND l.targetType = :type AND l.targetId = :targetId")
    Optional<Like.Reaction> findReactionByUserAndTarget(
            @Param("user") User user,
            @Param("type") Like.TargetType type,
            @Param("targetId") Long targetId);
}
