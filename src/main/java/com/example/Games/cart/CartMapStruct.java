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


@Mapper(componentModel = "spring")
public interface CartMapStruct {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "addedAt", ignore = true)
    CartItem createCartItem(User user, Game game);

    default CartSummaryResponse toCartSummaryResponse(List<CartItemResponse> items) {
        return CartSummaryResponse.from(items);
    }

    default CartOperationResponse toAddedResponse(Long gameId, String gameTitle, int cartSize) {
        return CartOperationResponse.addedToCart(gameId, gameTitle, cartSize);
    }

    default CartOperationResponse toRemovedResponse(Long gameId, String gameTitle, int cartSize) {
        return CartOperationResponse.removedFromCart(gameId, gameTitle, cartSize);
    }


    default CartOperationResponse toCheckedOutResponse(int itemsProcessed, BigDecimal totalAmount) {
        return CartOperationResponse.checkedOut(itemsProcessed, totalAmount);
    }

    default CartOperationResponse toClearedResponse(int itemsRemoved) {
        return CartOperationResponse.cartCleared(itemsRemoved);
    }

}
