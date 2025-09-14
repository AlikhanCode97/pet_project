package com.example.Games.cart;

import com.example.Games.cart.dto.AddToCartRequest;
import com.example.Games.cart.dto.CartItemResponse;
import com.example.Games.cart.dto.CartOperationResponse;
import com.example.Games.cart.dto.CartSummaryResponse;
import com.example.Games.config.exception.cart.GameAlreadyOwnedException;
import com.example.Games.config.exception.game.GameNotFoundException;
import com.example.Games.config.exception.cart.GameAlreadyInCartException;
import com.example.Games.config.exception.cart.CartOperationException;
import com.example.Games.config.common.service.UserContextService;
import com.example.Games.game.Game;
import com.example.Games.game.GameMapStruct;
import com.example.Games.game.GameRepository;
import com.example.Games.purchase.PurchaseRepository;
import com.example.Games.purchase.PurchaseService;
import com.example.Games.purchase.dto.PurchaseResponse;
import com.example.Games.user.auth.User;
import com.example.Games.user.balance.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final GameRepository gameRepository;
    private final UserContextService userContextService;
    private final PurchaseService purchaseService;
    private final BalanceService balanceService;
    private final GameMapStruct gameMapStruct;
    private final CartMapStruct cartMapper;
    private final PurchaseRepository purchaseRepository;

    private User getCurrentUser() {
        return userContextService.getCurrentUser();
    }

    @Transactional
    public CartOperationResponse addToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> GameNotFoundException.byId(request.gameId()));

        // Check if user already owns the game
        if (purchaseRepository.existsByUserIdAndGameId(user.getId(), game.getId())) {
            throw new GameAlreadyOwnedException("You already own this game: " + game.getTitle());
        }

        // Check if game is already in cart
        if (cartItemRepository.existsByUserAndGame(user, game)) {
            throw new GameAlreadyInCartException("Game is already in your cart: " + game.getTitle());
        }

        // Check if user is trying to add their own game
        if (game.getAuthor().getId().equals(user.getId())) {
            throw new IllegalStateException("You cannot add your own game to cart");
        }

        CartItem cartItem = cartMapper.createCartItem(user, game);
        cartItemRepository.save(cartItem);
        
        int cartSize = cartItemRepository.countByUser(user);
        log.info("Game '{}' (ID: {}) added to cart for user '{}'. Cart size: {}",
                game.getTitle(), game.getId(), user.getUsername(), cartSize);
        
        return cartMapper.toAddedResponse(game.getId(), game.getTitle(), cartSize);
    }

    @Transactional(readOnly = true)
    public CartSummaryResponse viewCart() {
        User user = getCurrentUser();
        List<CartItemResponse> items = cartItemRepository.findByUser(user)
                .stream()
                .map(cartItem -> new CartItemResponse(gameMapStruct.toDto(cartItem.getGame())))
                .toList();
        
        log.debug("Retrieved cart for user '{}' with {} items", user.getUsername(), items.size());
        return cartMapper.toCartSummaryResponse(items);
    }

    @Transactional
    public CartOperationResponse removeFromCart(Long gameId) {
        User user = getCurrentUser();
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> GameNotFoundException.byId(gameId));
        
        // Check if game exists in cart
        if (!cartItemRepository.existsByUserAndGame(user, game)) {
            throw CartOperationException.gameNotInCart(gameId);
        }
        
        cartItemRepository.deleteByUserAndGame(user, game);
        
        int cartSize = cartItemRepository.countByUser(user);
        log.info("Game '{}' (ID: {}) removed from cart for user '{}'. Cart size: {}",
                game.getTitle(), gameId, user.getUsername(), cartSize);
        
        return cartMapper.toRemovedResponse(game.getId(), game.getTitle(), cartSize);
    }

    @Transactional(rollbackFor = Exception.class)
    public CartOperationResponse checkout() {
        User user = getCurrentUser();
        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            throw CartOperationException.emptyCart();
        }

        // Extract games from cart items
        List<Game> games = cartItems.stream()
                .map(CartItem::getGame)
                .collect(Collectors.toList());

        BigDecimal total = games.stream()
                .map(Game::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Processing checkout for user '{}' with {} items, total: ${}",
                user.getUsername(), cartItems.size(), total);

        List<PurchaseResponse> purchaseResponses = purchaseService.purchaseGames(games, user);

        cartItemRepository.deleteAllByUser(user);
        
        log.info("Checkout completed successfully for user '{}'. {} games added to library, ${} charged",
                user.getUsername(), purchaseResponses.size(), total);
        
        return cartMapper.toCheckedOutResponse(purchaseResponses.size(), total);
    }

    @Transactional(readOnly = true)
    public int getCartItemCount() {
        User user = getCurrentUser();
        return cartItemRepository.countByUser(user);
    }

    @Transactional
    public CartOperationResponse clearCart() {
        User user = getCurrentUser();
        int itemCount = cartItemRepository.countByUser(user);

        if (itemCount == 0) {
            throw CartOperationException.emptyCart();
        }

        cartItemRepository.deleteAllByUser(user);
        log.info("Cleared cart for user '{}' - removed {} items", user.getUsername(), itemCount);

        return cartMapper.toClearedResponse(itemCount);
    }

    /**
     * Validate cart for checkout without actually purchasing
     * @return true if cart can be checked out successfully
     */
    @Transactional(readOnly = true)
    public boolean validateCartForCheckout() {
        User user = getCurrentUser();
        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            return false; // Empty cart can't be checked out
        }

        // Extract game IDs from cart items
        List<Long> gameIds = cartItems.stream()
                .map(cartItem -> cartItem.getGame().getId())
                .collect(Collectors.toList());

        // Use PurchaseService validation
        return purchaseService.canPurchaseGames(gameIds);
    }

    /**
     * Get detailed cart validation information
     * @return detailed validation results
     */
    @Transactional(readOnly = true)
    public CartValidationResponse getCartValidationDetails() {
        User user = getCurrentUser();
        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            return new CartValidationResponse(false, "Cart is empty", List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
        }

        List<Game> games = cartItems.stream()
                .map(CartItem::getGame)
                .collect(Collectors.toList());

        BigDecimal totalCost = games.stream()
                .map(Game::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal userBalance = balanceService.getRawBalance();
        
        List<String> issues = new ArrayList<>();
        
        // Check for owned games
        List<Long> ownedGameIds = purchaseService.getOwnedGames(
            games.stream().map(Game::getId).collect(Collectors.toList())
        );
        
        if (!ownedGameIds.isEmpty()) {
            List<String> ownedTitles = games.stream()
                    .filter(game -> ownedGameIds.contains(game.getId()))
                    .map(Game::getTitle)
                    .collect(Collectors.toList());
            issues.add("You already own: " + String.join(", ", ownedTitles));
        }
        
        // Check for own games
        List<String> ownGameTitles = games.stream()
                .filter(game -> game.getAuthor().getId().equals(user.getId()))
                .map(Game::getTitle)
                .collect(Collectors.toList());
        
        if (!ownGameTitles.isEmpty()) {
            issues.add("Cannot purchase your own games: " + String.join(", ", ownGameTitles));
        }
        
        // Check funds
        if (userBalance.compareTo(totalCost) < 0) {
            issues.add(String.format("Insufficient funds. Need $%.2f but have $%.2f", 
                    totalCost, userBalance));
        }
        
        boolean canCheckout = issues.isEmpty();
        String message = canCheckout ? "Cart is valid for checkout" : String.join("; ", issues);
        
        return new CartValidationResponse(canCheckout, message, issues, totalCost, userBalance);
    }

    // Helper record for cart validation
    public record CartValidationResponse(
            boolean canCheckout,
            String message,
            List<String> issues,
            BigDecimal totalCost,
            BigDecimal userBalance
    ) {}
}
