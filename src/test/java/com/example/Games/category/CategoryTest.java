package com.example.Games.category;

import com.example.Games.game.Game;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Category Entity Tests")
class CategoryTest {

    @Test
    @DisplayName("Should update name with validation")
    void shouldUpdateNameWithValidation() {
        // Given
        Category category = Category.builder().name("Action").build();
        
        // When & Then - Happy path
        category.updateName("Adventure");
        assertThat(category.getName()).isEqualTo("Adventure");
        
        // When & Then - Should trim whitespace
        category.updateName("  Horror  ");
        assertThat(category.getName()).isEqualTo("Horror");
        
        // When & Then - Should validate input
        assertThatThrownBy(() -> category.updateName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
                
        assertThatThrownBy(() -> category.updateName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
                
        assertThatThrownBy(() -> category.updateName("a".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 100 characters");
    }

    @Test
    @DisplayName("Should handle games collection correctly")
    void shouldHandleGamesCollection() {
        // Given
        Category category = Category.builder().games(new ArrayList<>()).build();
        
        // When & Then - Empty collection
        assertThat(category.hasGames()).isFalse();
        assertThat(category.getGameCount()).isEqualTo(0);
        
        // When & Then - Null collection
        category.setGames(null);
        assertThat(category.hasGames()).isFalse();
        assertThat(category.getGameCount()).isEqualTo(0);
        
        // When & Then - With games
        category.setGames(new ArrayList<>());
        category.getGames().add(Game.builder().id(1L).title("Game 1").build());
        category.getGames().add(Game.builder().id(2L).title("Game 2").build());
        
        assertThat(category.hasGames()).isTrue();
        assertThat(category.getGameCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should compare names case-insensitively")
    void shouldCompareNamesCaseInsensitively() {
        // Given
        Category category = Category.builder().name("Action").build();
        
        // When & Then - Case insensitive matches
        assertThat(category.isNameEqual("Action")).isTrue();
        assertThat(category.isNameEqual("action")).isTrue();
        assertThat(category.isNameEqual("ACTION")).isTrue();
        
        // When & Then - Different names
        assertThat(category.isNameEqual("RPG")).isFalse();
        assertThat(category.isNameEqual(null)).isFalse();
        
        // When & Then - Null category name
        category.setName(null);
        assertThat(category.isNameEqual("Action")).isFalse();
    }

    @Test
    @DisplayName("Should use ID for equality comparison")
    void shouldUseIdForEqualityComparison() {
        // Given
        Category category1 = Category.builder().id(1L).name("Action").build();
        Category category2 = Category.builder().id(1L).name("Different Name").build();
        Category category3 = Category.builder().id(2L).name("Action").build();
        
        // When & Then - Same ID should be equal
        assertThat(category1).isEqualTo(category2);
        assertThat(category1.hashCode()).isEqualTo(category2.hashCode());
        
        // When & Then - Different ID should not be equal
        assertThat(category1).isNotEqualTo(category3);
    }
}
