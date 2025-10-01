package com.example.Games.cart;

import com.example.Games.cart.dto.AddToCartRequest;
import com.example.Games.cart.dto.CartItemResponse;
import com.example.Games.cart.dto.CartOperationResponse;
import com.example.Games.cart.dto.CartSummaryResponse;
import com.example.Games.config.exception.purchase.GameAlreadyOwnedException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final GameMapStruct gameMapStruct;
    private final CartMapStruct cartMapper;
    private final PurchaseRepository purchaseRepository;

    private User getCurrentUser() {
        return userContextService.getAuthorizedUser();
    }

    @Transactional
    public CartOperationResponse addToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> GameNotFoundException.byId(request.gameId()));

        if (purchaseRepository.existsByUserIdAndGameId(user.getId(), game.getId())) {
            throw new GameAlreadyOwnedException("You already own this game: " + game.getTitle());
        }
        if (cartItemRepository.existsByUserAndGame(user, game)) {
            throw new GameAlreadyInCartException("Game is already in your cart: " + game.getTitle());
        }

        CartItem cartItem = cartMapper.createCartItem(user, game);
        cartItemRepository.save(cartItem);
        
        int cartSize = cartItemRepository.countByUser(user);
        log.info("Game '{}' (ID: {}) added to cart for user '{}'. Cart size: {}",
                game.getTitle(), game.getId(), user.getUsername(), cartSize);
        
        return cartMapper.toAddedResponse(game.getId(), game.getTitle(), cartSize);
    }

    @Transactional
    public CartOperationResponse removeFromCart(Long gameId) {
        User user = getCurrentUser();
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> GameNotFoundException.byId(gameId));

        if (!cartItemRepository.existsByUserAndGame(user, game)) {
            throw CartOperationException.gameNotInCart(gameId);
        }

        cartItemRepository.deleteByUserAndGame(user, game);
        int cartSize = cartItemRepository.countByUser(user);
        log.info("Game '{}' (ID: {}) removed from cart for user '{}'. Cart size: {}",
                game.getTitle(), gameId, user.getUsername(), cartSize);

        return cartMapper.toRemovedResponse(game.getId(), game.getTitle(), cartSize);
    }

    @Transactional(readOnly = true)
    public CartSummaryResponse viewCart() {
        User user = getCurrentUser();
        List<CartItemResponse> items = cartItemRepository.findByUserWithGame(user)
                .stream()
                .map(cartItem -> new CartItemResponse(gameMapStruct.toDto(cartItem.getGame())))
                .toList();
        
        log.debug("Retrieved cart for user '{}' with {} items", user.getUsername(), items.size());
        return cartMapper.toCartSummaryResponse(items);
    }

    @Transactional(rollbackFor = Exception.class)
    public CartOperationResponse checkout() {
        User user = getCurrentUser();
        List<CartItem> cartItems = cartItemRepository.findByUserWithGame(user);

        if (cartItems.isEmpty()) {
            throw CartOperationException.emptyCart();
        }

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

    @Transactional(readOnly = true)
    public boolean validateCartForCheckout() {
        User user = getCurrentUser();
        List<Long> gameIds = cartItemRepository.findGameIdsByUser(user);

        if (gameIds.isEmpty()) {
            return false;
        }
        return purchaseService.canPurchaseGames(gameIds);
    }
}
