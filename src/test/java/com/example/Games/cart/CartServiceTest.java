package com.example.Games.cart;

import com.example.Games.cart.dto.AddToCartRequest;
import com.example.Games.cart.dto.CartItemResponse;
import com.example.Games.cart.dto.CartOperationResponse;
import com.example.Games.cart.dto.CartSummaryResponse;
import com.example.Games.config.common.service.UserContextService;
import com.example.Games.config.exception.cart.CartOperationException;
import com.example.Games.config.exception.cart.GameAlreadyInCartException;
import com.example.Games.config.exception.game.GameNotFoundException;
import com.example.Games.config.exception.purchase.GameAlreadyOwnedException;
import com.example.Games.game.Game;
import com.example.Games.game.GameMapStruct;
import com.example.Games.game.GameRepository;
import com.example.Games.game.dto.Response;
import com.example.Games.purchase.PurchaseRepository;
import com.example.Games.purchase.PurchaseService;
import com.example.Games.purchase.dto.PurchaseResponse;
import com.example.Games.user.auth.User;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleType;
import com.example.Games.category.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Tests")
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserContextService userContextService;

    @Mock
    private PurchaseService purchaseService;

    @Mock
    private GameMapStruct gameMapStruct;

    @Mock
    private CartMapStruct cartMapper;

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private CartService cartService;

    @Captor
    private ArgumentCaptor<CartItem> cartItemCaptor;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<Game> gameCaptor;

    private User currentUser;
    private User developer;
    private Game testGame;
    private Game testGame2;
    private CartItem cartItem1;
    private CartItem cartItem2;
    private Category category;

    @BeforeEach
    void setUp() {
        // Setup roles
        Role userRole = Role.builder()
                .name(RoleType.USER)
                .build();

        Role developerRole = Role.builder()
                .name(RoleType.DEVELOPER)
                .build();

        // Setup users
        currentUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(userRole)
                .build();

        developer = User.builder()
                .id(2L)
                .username("developer")
                .email("dev@example.com")
                .role(developerRole)
                .build();

        // Setup category
        category = Category.builder()
                .id(1L)
                .name("Action")
                .build();

        // Setup games
        testGame = Game.builder()
                .id(1L)
                .title("Test Game")
                .author(developer)
                .price(new BigDecimal("29.99"))
                .category(category)
                .build();

        testGame2 = Game.builder()
                .id(2L)
                .title("Test Game 2")
                .author(developer)
                .price(new BigDecimal("39.99"))
                .category(category)
                .build();

        // Setup cart items
        cartItem1 = CartItem.builder()
                .id(1L)
                .user(currentUser)
                .game(testGame)
                .addedAt(LocalDateTime.now())
                .build();

        cartItem2 = CartItem.builder()
                .id(2L)
                .user(currentUser)
                .game(testGame2)
                .addedAt(LocalDateTime.now())
                .build();

        // Default mock behavior
        when(userContextService.getAuthorizedUser()).thenReturn(currentUser);
    }

    @Test
    @DisplayName("Should successfully add game to cart")
    void shouldSuccessfullyAddGameToCart() {
        // Given
        AddToCartRequest request = AddToCartRequest.of(1L);
        CartOperationResponse expectedResponse = CartOperationResponse.addedToCart(
                1L, "Test Game", 1
        );

        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        when(purchaseRepository.existsByUserIdAndGameId(1L, 1L)).thenReturn(false);
        when(cartItemRepository.existsByUserAndGame(currentUser, testGame)).thenReturn(false);
        when(cartMapper.createCartItem(currentUser, testGame)).thenReturn(cartItem1);
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem1);
        when(cartItemRepository.countByUser(currentUser)).thenReturn(1);
        when(cartMapper.toAddedResponse(1L, "Test Game", 1)).thenReturn(expectedResponse);

        // When
        CartOperationResponse result = cartService.addToCart(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.operation()).isEqualTo("ADD");
        assertThat(result.gameId()).isEqualTo(1L);
        assertThat(result.gameTitle()).isEqualTo("Test Game");
        assertThat(result.cartSize()).isEqualTo(1);

        verify(cartItemRepository).save(cartItemCaptor.capture());
        assertThat(cartItemCaptor.getValue()).isEqualTo(cartItem1);
    }

    @Test
    @DisplayName("Should throw exception when game not found for add to cart")
    void shouldThrowExceptionWhenGameNotFoundForAddToCart() {
        // Given
        AddToCartRequest request = AddToCartRequest.of(999L);
        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cartService.addToCart(request))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("999");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user already owns game")
    void shouldThrowExceptionWhenUserAlreadyOwnsGame() {
        // Given
        AddToCartRequest request = AddToCartRequest.of(1L);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        when(purchaseRepository.existsByUserIdAndGameId(1L, 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> cartService.addToCart(request))
                .isInstanceOf(GameAlreadyOwnedException.class)
                .hasMessage("You already own this game: Test Game");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when game already in cart")
    void shouldThrowExceptionWhenGameAlreadyInCart() {
        // Given
        AddToCartRequest request = AddToCartRequest.of(1L);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        when(purchaseRepository.existsByUserIdAndGameId(1L, 1L)).thenReturn(false);
        when(cartItemRepository.existsByUserAndGame(currentUser, testGame)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> cartService.addToCart(request))
                .isInstanceOf(GameAlreadyInCartException.class)
                .hasMessage("Game is already in your cart: Test Game");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully remove game from cart")
    void shouldSuccessfullyRemoveGameFromCart() {
        // Given
        CartOperationResponse expectedResponse = CartOperationResponse.removedFromCart(
                1L, "Test Game", 0
        );

        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        when(cartItemRepository.existsByUserAndGame(currentUser, testGame)).thenReturn(true);
        doNothing().when(cartItemRepository).deleteByUserAndGame(currentUser, testGame);
        when(cartItemRepository.countByUser(currentUser)).thenReturn(0);
        when(cartMapper.toRemovedResponse(1L, "Test Game", 0)).thenReturn(expectedResponse);

        // When
        CartOperationResponse result = cartService.removeFromCart(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.operation()).isEqualTo("REMOVE");
        assertThat(result.gameId()).isEqualTo(1L);
        assertThat(result.cartSize()).isEqualTo(0);

        verify(cartItemRepository).deleteByUserAndGame(userCaptor.capture(), gameCaptor.capture());
        assertThat(userCaptor.getValue()).isEqualTo(currentUser);
        assertThat(gameCaptor.getValue()).isEqualTo(testGame);
    }

    @Test
    @DisplayName("Should throw exception when game not in cart for removal")
    void shouldThrowExceptionWhenGameNotInCartForRemoval() {
        // Given
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        when(cartItemRepository.existsByUserAndGame(currentUser, testGame)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> cartService.removeFromCart(1L))
                .isInstanceOf(CartOperationException.class)
                .hasMessageContaining("1");

        verify(cartItemRepository, never()).deleteByUserAndGame(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when game not found for removal")
    void shouldThrowExceptionWhenGameNotFoundForRemoval() {
        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeFromCart(999L))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("999");

        verify(cartItemRepository, never()).deleteByUserAndGame(any(), any());
    }


    @Test
    @DisplayName("Should successfully view cart with items")
    void shouldSuccessfullyViewCartWithItems() {
        // Given
        Response gameResponse1 = Response.builder()
                .id(1L)
                .title("Test Game")
                .price(new BigDecimal("29.99"))
                .build();

        Response gameResponse2 = Response.builder()
                .id(2L)
                .title("Test Game 2")
                .price(new BigDecimal("39.99"))
                .build();

        CartItemResponse cartItemResponse1 = new CartItemResponse(gameResponse1);
        CartItemResponse cartItemResponse2 = new CartItemResponse(gameResponse2);

        List<CartItem> cartItems = Arrays.asList(cartItem1, cartItem2);
        List<CartItemResponse> cartItemResponses = Arrays.asList(cartItemResponse1, cartItemResponse2);

        CartSummaryResponse expectedResponse = CartSummaryResponse.from(cartItemResponses);

        when(cartItemRepository.findByUserWithGame(currentUser)).thenReturn(cartItems);
        when(gameMapStruct.toDto(testGame)).thenReturn(gameResponse1);
        when(gameMapStruct.toDto(testGame2)).thenReturn(gameResponse2);
        when(cartMapper.toCartSummaryResponse(anyList())).thenReturn(expectedResponse);

        // When
        CartSummaryResponse result = cartService.viewCart();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalItems()).isEqualTo(2);
        assertThat(result.totalPrice()).isEqualByComparingTo(new BigDecimal("69.98"));
        assertThat(result.items()).hasSize(2);

        verify(cartItemRepository).findByUserWithGame(currentUser);
    }

    @Test
    @DisplayName("Should handle empty cart view")
    void shouldHandleEmptyCartView() {
        // Given
        CartSummaryResponse expectedResponse = CartSummaryResponse.from(Collections.emptyList());

        when(cartItemRepository.findByUserWithGame(currentUser)).thenReturn(Collections.emptyList());
        when(cartMapper.toCartSummaryResponse(anyList())).thenReturn(expectedResponse);

        // When
        CartSummaryResponse result = cartService.viewCart();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalItems()).isEqualTo(0);
        assertThat(result.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("Should successfully checkout cart")
    void shouldSuccessfullyCheckoutCart() {
        // Given
        List<CartItem> cartItems = Arrays.asList(cartItem1, cartItem2);
        List<Game> games = Arrays.asList(testGame, testGame2);

        PurchaseResponse purchaseResponse1 = PurchaseResponse.builder()
                .purchaseId(1L)
                .gameTitle("Test Game")
                .purchasePrice(new BigDecimal("29.99"))
                .build();

        PurchaseResponse purchaseResponse2 = PurchaseResponse.builder()
                .purchaseId(2L)
                .gameTitle("Test Game 2")
                .purchasePrice(new BigDecimal("39.99"))
                .build();

        List<PurchaseResponse> purchaseResponses = Arrays.asList(purchaseResponse1, purchaseResponse2);
        BigDecimal totalAmount = new BigDecimal("69.98");

        CartOperationResponse expectedResponse = CartOperationResponse.checkedOut(2, totalAmount);

        when(cartItemRepository.findByUserWithGame(currentUser)).thenReturn(cartItems);
        when(purchaseService.purchaseGames(games, currentUser)).thenReturn(purchaseResponses);
        doNothing().when(cartItemRepository).deleteAllByUser(currentUser);
        when(cartMapper.toCheckedOutResponse(2, totalAmount)).thenReturn(expectedResponse);

        // When
        CartOperationResponse result = cartService.checkout();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.operation()).isEqualTo("CHECKOUT");
        assertThat(result.itemsProcessed()).isEqualTo(2);
        assertThat(result.totalAmount()).isEqualByComparingTo(totalAmount);

        verify(purchaseService).purchaseGames(eq(games), eq(currentUser));
        verify(cartItemRepository).deleteAllByUser(currentUser);
    }

    @Test
    @DisplayName("Should throw exception when checking out empty cart")
    void shouldThrowExceptionWhenCheckingOutEmptyCart() {
        // Given
        when(cartItemRepository.findByUserWithGame(currentUser)).thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> cartService.checkout())
                .isInstanceOf(CartOperationException.class)
                .hasMessageContaining("empty");

        verify(purchaseService, never()).purchaseGames(any(), any());
        verify(cartItemRepository, never()).deleteAllByUser(any());
    }

    @Test
    @DisplayName("Should successfully clear cart")
    void shouldSuccessfullyClearCart() {
        // Given
        CartOperationResponse expectedResponse = CartOperationResponse.cartCleared(2);

        when(cartItemRepository.countByUser(currentUser)).thenReturn(2);
        doNothing().when(cartItemRepository).deleteAllByUser(currentUser);
        when(cartMapper.toClearedResponse(2)).thenReturn(expectedResponse);

        // When
        CartOperationResponse result = cartService.clearCart();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.operation()).isEqualTo("CLEAR");
        assertThat(result.itemsProcessed()).isEqualTo(2);
        assertThat(result.cartSize()).isEqualTo(0);

        verify(cartItemRepository).deleteAllByUser(currentUser);
    }

    @Test
    @DisplayName("Should throw exception when clearing empty cart")
    void shouldThrowExceptionWhenClearingEmptyCart() {
        // Given
        when(cartItemRepository.countByUser(currentUser)).thenReturn(0);

        // When & Then
        assertThatThrownBy(() -> cartService.clearCart())
                .isInstanceOf(CartOperationException.class)
                .hasMessageContaining("empty");

        verify(cartItemRepository, never()).deleteAllByUser(any());
    }

    @Test
    @DisplayName("Should validate cart for checkout successfully")
    void shouldValidateCartForCheckoutSuccessfully() {
        // Given
        List<Long> gameIds = Arrays.asList(1L, 2L);

        when(cartItemRepository.findGameIdsByUser(currentUser)).thenReturn(gameIds);
        when(purchaseService.canPurchaseGames(gameIds)).thenReturn(true);

        // When
        boolean result = cartService.validateCartForCheckout();

        // Then
        assertThat(result).isTrue();
        verify(purchaseService).canPurchaseGames(gameIds);
    }

    @Test
    @DisplayName("Should return false for empty cart validation")
    void shouldReturnFalseForEmptyCartValidation() {
        // Given
        when(cartItemRepository.findGameIdsByUser(currentUser)).thenReturn(Collections.emptyList());

        // When
        boolean result = cartService.validateCartForCheckout();

        // Then
        assertThat(result).isFalse();
        verify(purchaseService, never()).canPurchaseGames(any());
    }
}