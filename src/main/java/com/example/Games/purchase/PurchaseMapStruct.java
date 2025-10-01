package com.example.Games.purchase;

import com.example.Games.game.Game;
import com.example.Games.purchase.dto.PurchaseResponse;
import com.example.Games.user.auth.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PurchaseMapStruct {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "purchasedAt", ignore = true)
    @Mapping(target = "purchasePrice", source = "game.price")
    @Mapping(target = "game", source = "game")
    PurchaseHistory createPurchase(User user, Game game);

    @Mapping(target = "purchaseId", source = "id")
    @Mapping(target = "gameId", source = "game.id")
    @Mapping(target = "gameTitle", source = "game.title")
    @Mapping(target = "gameAuthor", source = "game.author.username")
    @Mapping(target = "purchasePrice", source = "purchasePrice")
    @Mapping(target = "currentGamePrice", source = "game.price")
    @Mapping(target = "purchasedAt", source = "purchasedAt")
    @Mapping(target = "priceDifference", expression = "java(calculatePriceDifference(purchase.getGame().getPrice(), purchase.getPurchasePrice()))")
    PurchaseResponse toPurchaseResponse(PurchaseHistory purchase);

    List<PurchaseResponse> toPurchaseResponseList(List<PurchaseHistory> purchases);

    default BigDecimal calculatePriceDifference(BigDecimal currentPrice, BigDecimal purchasePrice) {
        if (currentPrice == null || purchasePrice == null) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(purchasePrice);
    }

    default String formatPrice(BigDecimal price) {
        return String.format("$%.2f", price != null ? price : BigDecimal.ZERO);
    }
}
