package com.blogplatform.repository;

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
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Page<User> findByRole(User.Role role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime date);

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :now WHERE u.id = :id")
    void updateLastLogin(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.id = :id")
    void updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);

    @Modifying
    @Query("UPDATE User u SET u.locked = :locked WHERE u.id = :id")
    void updateLocked(@Param("id") Long id, @Param("locked") boolean locked);
}
