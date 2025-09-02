package com.example.Games.cart;

import com.example.Games.cart.dto.AddToCartRequest;
import com.example.Games.cart.dto.CartItemResponse;
import com.example.Games.config.exception.ResourceNotFoundException;
import com.example.Games.game.Game;
import com.example.Games.game.GameMapStruct;
import com.example.Games.game.GameRepository;
import com.example.Games.user.User;
import com.example.Games.user.UserRepository;
import com.example.Games.user.balance.Balance;
import com.example.Games.user.balance.BalanceRepository;
import com.example.Games.user.userLibrary.UserLibrary;
import com.example.Games.user.userLibrary.UserLibraryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final BalanceRepository balanceRepository;
    private final GameMapStruct gameMapStruct;

    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public void addToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        if (userLibraryRepository.existsByUserIdAndGameId(user.getId(), game.getId())) {
            throw new IllegalStateException("Game already in library");
        }

        if (!cartItemRepository.existsByUserAndGame(user, game)) {
            cartItemRepository.save(new CartItem(null, user, game));
        }
    }

    public List<CartItemResponse> viewCart() {
        User user = getCurrentUser();
        return cartItemRepository.findByUser(user)
                .stream()
                .map(cartItem -> new CartItemResponse(gameMapStruct.toDto(cartItem.getGame())))
                .toList();
    }

    public void removeFromCart(Long gameId) {
        User user = getCurrentUser();
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));
        cartItemRepository.deleteByUserAndGame(user, game);
    }

    @Transactional
    public void checkout() {
        User user = getCurrentUser();
        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        Balance balance = balanceRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Balance not found"));

        BigDecimal total = cartItems.stream()
                .map(item -> item.getGame().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (balance.getAmount().compareTo(total) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        balance.withdraw(total);
        balanceRepository.save(balance);

        cartItems.forEach(item -> {
            UserLibrary entry = UserLibrary.of(user, item.getGame());
            userLibraryRepository.save(entry);
        });

        cartItemRepository.deleteAllByUser(user);
    }
}
