package com.blogplatform.service;

import com.blogplatform.dto.CategoryDto;
import com.blogplatform.dto.PagedResponse;
import com.blogplatform.exception.*;
import com.blogplatform.model.Category;
import com.blogplatform.repository.CategoryRepository;
import com.blogplatform.repository.PostRepository;
import com.blogplatform.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final SlugUtil slugUtil;

    @Cacheable("categories")
    public List<CategoryDto.Detail> getAllCategories() {
        return categoryRepository.findAll(Sort.by("name"))
                .stream().map(this::mapToDetail).toList();
    }

    public PagedResponse<CategoryDto.Detail> getCategories(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<Category> result = (search != null && !search.isBlank())
                ? categoryRepository.search(search, pageable)
                : categoryRepository.findAll(pageable);
        return PagedResponse.of(result.map(this::mapToDetail));
    }

    public CategoryDto.Detail getCategoryBySlug(String slug) {
        return mapToDetail(findBySlug(slug));
    }

    public CategoryDto.Detail getCategoryById(Long id) {
        return mapToDetail(findById(id));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDto.Detail createCategory(CategoryDto.CreateRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category already exists: " + request.getName());
        }
        String slug = slugUtil.makeUnique(
                slugUtil.toSlug(request.getName()),
                categoryRepository::existsBySlug);

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .build();
        return mapToDetail(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDto.Detail updateCategory(Long id, CategoryDto.UpdateRequest request) {
        Category category = findById(id);
        if (request.getName() != null) {
            if (!request.getName().equals(category.getName())
                    && categoryRepository.existsByName(request.getName())) {
                throw new DuplicateResourceException("Category name already in use: " + request.getName());
            }
            category.setName(request.getName());
            category.setSlug(slugUtil.makeUnique(
                    slugUtil.toSlug(request.getName()),
                    s -> !s.equals(category.getSlug()) && categoryRepository.existsBySlug(s)));
        }
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        return mapToDetail(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        categoryRepository.delete(findById(id));
    }

    // ---- Mapping ----

    public CategoryDto.Summary mapToSummary(Category c) {
        if (c == null) return null;
        return CategoryDto.Summary.builder()
                .id(c.getId()).name(c.getName()).slug(c.getSlug()).description(c.getDescription())
                .build();
    }

    private CategoryDto.Detail mapToDetail(Category c) {
        return CategoryDto.Detail.builder()
                .id(c.getId()).name(c.getName()).slug(c.getSlug()).description(c.getDescription())
                .postCount((long) (c.getPosts() != null ? c.getPosts().size() : 0))
                .createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt())
                .build();
    }

    private Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    private Category findBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
    }
}
