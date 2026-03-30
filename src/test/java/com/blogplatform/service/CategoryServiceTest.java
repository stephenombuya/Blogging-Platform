package com.blogplatform.service;

import com.blogplatform.dto.CategoryDto;
import com.blogplatform.exception.DuplicateResourceException;
import com.blogplatform.model.Category;
import com.blogplatform.repository.CategoryRepository;
import com.blogplatform.repository.PostRepository;
import com.blogplatform.util.SlugUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Tests")
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock PostRepository     postRepository;
    @Spy  SlugUtil           slugUtil = new SlugUtil();

    @InjectMocks CategoryService categoryService;

    private Category existing;

    @BeforeEach
    void setUp() {
        existing = Category.builder()
                .id(1L).name("Technology").slug("technology")
                .description("Tech posts")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .posts(List.of())
                .build();
    }

    @Test
    @DisplayName("createCategory: success")
    void createCategory_success() {
        CategoryDto.CreateRequest req = new CategoryDto.CreateRequest();
        req.setName("Science");
        req.setDescription("Science posts");

        when(categoryRepository.existsByName("Science")).thenReturn(false);
        when(categoryRepository.existsBySlug(any())).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c = Category.builder().id(2L).name(c.getName()).slug(c.getSlug())
                    .description(c.getDescription()).posts(List.of())
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            return c;
        });

        CategoryDto.Detail result = categoryService.createCategory(req);

        assertThat(result.getName()).isEqualTo("Science");
        assertThat(result.getSlug()).isEqualTo("science");
    }

    @Test
    @DisplayName("createCategory: throws DuplicateResourceException when name exists")
    void createCategory_duplicate_throws() {
        CategoryDto.CreateRequest req = new CategoryDto.CreateRequest();
        req.setName("Technology");

        when(categoryRepository.existsByName("Technology")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("getAllCategories: returns sorted list")
    void getAllCategories_returnsList() {
        when(categoryRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(existing));

        List<CategoryDto.Detail> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Technology");
    }

    @Test
    @DisplayName("getCategoryBySlug: returns category detail")
    void getCategoryBySlug_found() {
        when(categoryRepository.findBySlug("technology")).thenReturn(Optional.of(existing));

        CategoryDto.Detail result = categoryService.getCategoryBySlug("technology");

        assertThat(result.getSlug()).isEqualTo("technology");
        assertThat(result.getName()).isEqualTo("Technology");
    }

    @Test
    @DisplayName("deleteCategory: delegates to repository")
    void deleteCategory_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatNoException().isThrownBy(() -> categoryService.deleteCategory(1L));
        verify(categoryRepository).delete(existing);
    }
}
