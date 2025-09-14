package com.example.Games.cart;

import com.example.Games.cart.dto.CartOperationResponse;
import com.example.Games.cart.dto.CartSummaryResponse;
import com.example.Games.cart.dto.CartItemResponse;
import com.example.Games.game.Game;
import com.example.Games.user.auth.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapStruct mapper for cart-related responses and entities
 * Handles transformation between service data and response DTOs
 */
@Mapper(componentModel = "spring")
public interface CartMapStruct {

    /**
     * Factory method for creating CartItem entities
     * @param user the user adding to cart
     * @param game the game being added
     * @return new CartItem entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "addedAt", ignore = true)
    CartItem createCartItem(User user, Game game);

    /**
     * Convert cart items list to summary response with calculated totals
     * @param items list of cart item responses
     * @return cart summary with totals and formatted prices
     */
    default CartSummaryResponse toCartSummaryResponse(List<CartItemResponse> items) {
        return CartSummaryResponse.from(items);
    }

    /**
     * Create operation response for successful game addition
     * @param gameId the game ID that was added
     * @param gameTitle the game title for display
     * @param cartSize updated cart size
     * @return formatted operation response
     */
    default CartOperationResponse toAddedResponse(Long gameId, String gameTitle, int cartSize) {
        return CartOperationResponse.addedToCart(gameId, gameTitle, cartSize);
    }

    /**
     * Create operation response for successful game removal
     * @param gameId the game ID that was removed
     * @param gameTitle the game title for display
     * @param cartSize updated cart size
     * @return formatted operation response
     */
    default CartOperationResponse toRemovedResponse(Long gameId, String gameTitle, int cartSize) {
        return CartOperationResponse.removedFromCart(gameId, gameTitle, cartSize);
    }

    /**
     * Create operation response for successful checkout
     * @param itemsProcessed number of games purchased
     * @return formatted checkout response
     */
    default CartOperationResponse toCheckedOutResponse(int itemsProcessed) {
        return CartOperationResponse.checkedOut(itemsProcessed);
    }

    /**
     * Create operation response for successful checkout with total amount
     * @param itemsProcessed number of games purchased
     * @param totalAmount total amount charged
     * @return formatted checkout response with purchase details
     */
    default CartOperationResponse toCheckedOutResponse(int itemsProcessed, BigDecimal totalAmount) {
        return CartOperationResponse.checkedOut(itemsProcessed, totalAmount);
    }

    /**
     * Create operation response for successful cart clearing
     * @param itemsRemoved number of items removed from cart
     * @return formatted clear cart response
     */
    default CartOperationResponse toClearedResponse(int itemsRemoved) {
        return CartOperationResponse.cartCleared(itemsRemoved);
    }

    /**
     * Helper method for consistent price formatting across cart operations
     * @param price the price to format
     * @return formatted price string with currency symbol
     */
    default String formatPrice(BigDecimal price) {
        return String.format("$%.2f", price != null ? price : BigDecimal.ZERO);
    }

    /**
     * Create empty cart summary response
     * @return empty cart response with zero values
     */
    default CartSummaryResponse emptyCartResponse() {
        return new CartSummaryResponse(
                List.of(),
                0,
                BigDecimal.ZERO,
                formatPrice(BigDecimal.ZERO)
        );
    }
}
