package com.blogplatform.repository;

import com.blogplatform.model.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findBySlug(String slug);
    Optional<Tag> findByName(String name);
    boolean existsByName(String name);
    boolean existsBySlug(String slug);

    List<Tag> findByNameIn(List<String> names);

    @Query("SELECT t FROM Tag t ORDER BY SIZE(t.posts) DESC")
    Page<Tag> findPopularTags(Pageable pageable);

    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Tag> searchByName(@Param("q") String query);
}
