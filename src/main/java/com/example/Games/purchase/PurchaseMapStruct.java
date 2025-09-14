package com.example.Games.purchase;

import com.example.Games.game.Game;
import com.example.Games.purchase.dto.PurchaseEligibilityResponse;
import com.example.Games.purchase.dto.PurchaseResponse;
import com.example.Games.purchase.dto.PurchaseStatsResponse;
import com.example.Games.user.auth.User;
import com.example.Games.user.balance.dto.BalanceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapStruct mapper for purchase-related responses and entities
 */
@Mapper(componentModel = "spring")
public interface PurchaseMapStruct {
    
    // Factory method for creating purchase entities
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "purchasedAt", ignore = true)
    @Mapping(target = "purchasePrice", source = "game.price")
    @Mapping(target = "game", source = "game")
    PurchaseHistory createPurchase(User user, Game game);
    
    // Map purchase eligibility response
    @Mapping(target = "eligible", source = "eligible")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "gameId", source = "gameId")
    @Mapping(target = "gameTitle", source = "gameTitle")
    @Mapping(target = "gamePrice", source = "gamePrice")
    @Mapping(target = "formattedGamePrice", expression = "java(formatPrice(gamePrice))")
    @Mapping(target = "userBalance", expression = "java(userBalance.balance())")
    @Mapping(target = "formattedUserBalance", expression = "java(formatPrice(userBalance.balance()))")
    @Mapping(target = "sufficientFunds", source = "sufficientFunds")
    PurchaseEligibilityResponse toPurchaseEligibilityResponse(
            boolean eligible,
            String message,
            Long gameId,
            String gameTitle,
            BigDecimal gamePrice,
            BalanceResponse userBalance,
            boolean sufficientFunds
    );
    
    // Map purchase history to response
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
    
    // Map purchase statistics response
    default PurchaseStatsResponse toPurchaseStatsResponse(
            int totalGamesOwned,
            BigDecimal totalMoneySpent,
            BigDecimal averagePrice,
            java.time.LocalDateTime firstPurchaseDate,
            java.time.LocalDateTime lastPurchaseDate) {
        
        return PurchaseStatsResponse.builder()
                .totalGamesOwned(totalGamesOwned)
                .totalMoneySpent(totalMoneySpent)
                .averagePrice(averagePrice)
                .firstPurchaseDate(firstPurchaseDate)
                .lastPurchaseDate(lastPurchaseDate)
                .build();
    }
    
    // Helper method for price difference calculation
    default BigDecimal calculatePriceDifference(BigDecimal currentPrice, BigDecimal purchasePrice) {
        if (currentPrice == null || purchasePrice == null) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(purchasePrice);
    }

    // Helper method for price formatting
    default String formatPrice(BigDecimal price) {
        return String.format("$%.2f", price != null ? price : BigDecimal.ZERO);
    }
}
