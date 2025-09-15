package com.example.Games.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CategoryRepository Integration Tests")
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category actionCategory;
    private Category rpgCategory;

    @BeforeEach
    void setUp() {
        // Create test categories
        actionCategory = Category.builder()
                .name("Action")
                .build();

        rpgCategory = Category.builder()
                .name("RPG")
                .build();

        // Save them using EntityManager for better control
        entityManager.persistAndFlush(actionCategory);
        entityManager.persistAndFlush(rpgCategory);
    }

    @Test
    @DisplayName("Should find category by name (case sensitive)")
    void shouldFindCategoryByName() {
        // When
        Optional<Category> found = categoryRepository.findByName("Action");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Action");
        assertThat(found.get().getId()).isEqualTo(actionCategory.getId());
    }

    @Test
    @DisplayName("Should return empty when category name not found")
    void shouldReturnEmptyWhenCategoryNameNotFound() {
        // When
        Optional<Category> found = categoryRepository.findByName("NonExistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should be case sensitive when finding by name")
    void shouldBeCaseSensitiveWhenFindingByName() {
        // When
        Optional<Category> foundLowerCase = categoryRepository.findByName("action");
        Optional<Category> foundUpperCase = categoryRepository.findByName("ACTION");

        // Then
        assertThat(foundLowerCase).isEmpty();
        assertThat(foundUpperCase).isEmpty();
    }

    @Test
    @DisplayName("Should find all categories")
    void shouldFindAllCategories() {
        // When
        List<Category> categories = categoryRepository.findAll();

        // Then
        assertThat(categories).hasSize(2);
        assertThat(categories)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Action", "RPG");
    }

    @Test
    @DisplayName("Should save new category")
    void shouldSaveNewCategory() {
        // Given
        Category strategyCategory = Category.builder()
                .name("Strategy")
                .build();

        // When
        Category saved = categoryRepository.save(strategyCategory);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Strategy");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        // Verify it's persisted
        Optional<Category> found = categoryRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Strategy");
    }

    @Test
    @DisplayName("Should update existing category")
    void shouldUpdateExistingCategory() {
        // Given
        actionCategory.updateName("Action Updated");

        // When
        Category updated = categoryRepository.save(actionCategory);

        // Then
        assertThat(updated.getName()).isEqualTo("Action Updated");
        assertThat(updated.getId()).isEqualTo(actionCategory.getId());

        // Verify in database
        Optional<Category> found = categoryRepository.findById(actionCategory.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Action Updated");
    }

    @Test
    @DisplayName("Should delete category by ID")
    void shouldDeleteCategoryById() {
        // Given
        Long categoryId = actionCategory.getId();

        // When
        categoryRepository.deleteById(categoryId);
        entityManager.flush();

        // Then
        Optional<Category> found = categoryRepository.findById(categoryId);
        assertThat(found).isEmpty();

        // Verify only one category remains
        List<Category> remaining = categoryRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getName()).isEqualTo("RPG");
    }

    @Test
    @DisplayName("Should find category by ID")
    void shouldFindCategoryById() {
        // When
        Optional<Category> found = categoryRepository.findById(actionCategory.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Action");
        assertThat(found.get().getId()).isEqualTo(actionCategory.getId());
    }

    @Test
    @DisplayName("Should return empty when finding by non-existent ID")
    void shouldReturnEmptyWhenFindingByNonExistentId() {
        // When
        Optional<Category> found = categoryRepository.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should enforce unique constraint on name")
    void shouldEnforceUniqueConstraintOnName() {
        // Given
        Category duplicateCategory = Category.builder()
                .name("Action") // Same name as existing category
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            categoryRepository.save(duplicateCategory);
            entityManager.flush(); // Force the constraint check
        }).hasMessageContaining("constraint");
    }

    @Test
    @DisplayName("Should handle null name gracefully in findByName")
    void shouldHandleNullNameGracefullyInFindByName() {
        // When
        Optional<Category> found = categoryRepository.findByName(null);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty string in findByName")
    void shouldHandleEmptyStringInFindByName() {
        // When
        Optional<Category> found = categoryRepository.findByName("");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should count categories correctly")
    void shouldCountCategoriesCorrectly() {
        // When
        long count = categoryRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return true when category exists by ID")
    void shouldReturnTrueWhenCategoryExistsById() {
        // When
        boolean exists = categoryRepository.existsById(actionCategory.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when category does not exist by ID")
    void shouldReturnFalseWhenCategoryDoesNotExistById() {
        // When
        boolean exists = categoryRepository.existsById(999L);

        // Then
        assertThat(exists).isFalse();
    }
}
