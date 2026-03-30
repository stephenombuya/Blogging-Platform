package com.blogplatform.service;

import com.blogplatform.dto.TagDto;
import com.blogplatform.exception.*;
import com.blogplatform.model.Tag;
import com.blogplatform.repository.TagRepository;
import com.blogplatform.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final SlugUtil slugUtil;

    public List<TagDto> getPopularTags(int limit) {
        return tagRepository.findPopularTags(PageRequest.of(0, limit))
                .stream().map(this::mapToDto).toList();
    }

    public List<TagDto> searchTags(String query) {
        return tagRepository.searchByName(query).stream().map(this::mapToDto).toList();
    }

    public TagDto getTagBySlug(String slug) {
        return mapToDto(findBySlug(slug));
    }

    @Transactional
    public Tag findOrCreate(String name) {
        String trimmed = name.trim();
        return tagRepository.findByName(trimmed).orElseGet(() -> {
            String slug = slugUtil.makeUnique(slugUtil.toSlug(trimmed), tagRepository::existsBySlug);
            return tagRepository.save(Tag.builder().name(trimmed).slug(slug).build());
        });
    }

    @Transactional
    public List<Tag> findOrCreateAll(List<String> names) {
        if (names == null || names.isEmpty()) return List.of();
        return names.stream().map(this::findOrCreate).toList();
    }

    @Transactional
    public void deleteTag(Long id) {
        tagRepository.delete(tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id)));
    }

    public TagDto mapToDto(Tag tag) {
        return TagDto.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .postCount((long) (tag.getPosts() != null ? tag.getPosts().size() : 0))
                .build();
    }

    private Tag findBySlug(String slug) {
        return tagRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "slug", slug));
    }
}
