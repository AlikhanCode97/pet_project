package com.example.Games.purchase;

import com.example.Games.game.Game;
import com.example.Games.game.GameRepository;
import com.example.Games.game.GameMapStruct;
import com.example.Games.game.dto.Response;
import com.example.Games.gameHistory.GameHistoryService;
import com.example.Games.purchase.dto.PurchaseResponse;
import com.example.Games.purchase.dto.PurchaseStatsResponse;
import com.example.Games.purchase.dto.PurchaseEligibilityResponse;
import com.example.Games.user.auth.User;
import com.example.Games.user.auth.UserRepository;
import com.example.Games.user.balance.BalanceService;
import com.example.Games.user.balance.transaction.BalanceTransaction;
import com.example.Games.user.balance.dto.BalanceResponse;
import com.example.Games.config.exception.ResourceNotFoundException;
import com.example.Games.config.exception.cart.GameAlreadyOwnedException;
import com.example.Games.config.exception.purchase.InsufficientFundsException;
import com.example.Games.config.exception.purchase.PurchaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final BalanceService balanceService;
    private final GameHistoryService gameHistoryService;
    private final GameMapStruct gameMapStruct;
    private final PurchaseMapStruct purchaseMapper;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    @Transactional
    public PurchaseResponse purchaseGame(Long gameId) {
        User currentUser = getCurrentUser();
        
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        validateSinglePurchase(currentUser, game);

        PurchaseHistory purchase = purchaseMapper.createPurchase(currentUser, game);
        purchase = purchaseRepository.save(purchase);

        BalanceTransaction balanceTransaction = balanceService.createPurchaseTransaction(game.getPrice());
        gameHistoryService.recordGamePurchase(game, currentUser, game.getPrice());

        log.info("Game '{}' purchased by user '{}' for ${} - Purchase ID: {}, Transaction ID: {}", 
                game.getTitle(), currentUser.getUsername(), game.getPrice(), 
                purchase.getId(), balanceTransaction.getId());

        return purchaseMapper.toPurchaseResponse(purchase);
    }

    @Transactional
    public List<PurchaseResponse> purchaseGamesByIds(List<Long> gameIds) {
        if (gameIds == null || gameIds.isEmpty()) {
            throw new PurchaseException("No games provided for purchase");
        }

        User currentUser = getCurrentUser();
        
        List<Game> games = gameRepository.findAllById(gameIds);
        if (games.size() != gameIds.size()) {
            throw new ResourceNotFoundException("One or more games not found");
        }
        
        return purchaseGames(games, currentUser);
    }

    @Transactional
    public List<PurchaseResponse> purchaseGames(List<Game> games, User user) {
        validateBatchPurchase(user, games);

        BigDecimal totalCost = games.stream()
                .map(Game::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!balanceService.hasSufficientFunds(totalCost)) {
            throw new InsufficientFundsException(
                String.format("Insufficient funds. Total cost: $%.2f, Available: $%.2f", 
                            totalCost, balanceService.getBalance().balance()));
        }

        List<PurchaseHistory> purchases = new ArrayList<>();
        for (Game game : games) {
            PurchaseHistory purchase = purchaseMapper.createPurchase(user, game);
            purchases.add(purchase);
        }

        purchases = purchaseRepository.saveAll(purchases);
        BalanceTransaction balanceTransaction = balanceService.createPurchaseTransaction(totalCost);
        gameHistoryService.recordGamePurchases(games, user);

        log.info("Batch purchase completed for user '{}': {} games purchased for ${} - Transaction ID: {}", 
                user.getUsername(), games.size(), totalCost, balanceTransaction.getId());

        return purchases.stream()
                .map(purchaseMapper::toPurchaseResponse)
                .collect(Collectors.toList());
    }

    // ============= PURCHASE VALIDATION =============

    private void validateSinglePurchase(User user, Game game) {
        if (purchaseRepository.existsByUserIdAndGameId(user.getId(), game.getId())) {
            throw new GameAlreadyOwnedException("You already own this game: " + game.getTitle());
        }

        if (game.getAuthor().getId().equals(user.getId())) {
            throw new PurchaseException("You cannot purchase your own game: " + game.getTitle());
        }

        if (!balanceService.hasSufficientFunds(game.getPrice())) {
            throw new InsufficientFundsException(String.format(
                    "Insufficient funds. Game costs $%.2f but you only have $%.2f", 
                    game.getPrice(), balanceService.getBalance().balance()));
        }
    }

    private void validateBatchPurchase(User user, List<Game> games) {
        for (Game game : games) {
            if (purchaseRepository.existsByUserIdAndGameId(user.getId(), game.getId())) {
                throw new GameAlreadyOwnedException(
                    String.format("You already own '%s'. Remove it from your cart first.", game.getTitle()));
            }
            
            if (game.getAuthor().getId().equals(user.getId())) {
                throw new PurchaseException(
                    String.format("Cannot purchase your own game: '%s'", game.getTitle()));
            }
        }
    }

    // ============= PURCHASE ELIGIBILITY (Used by Cart) =============

    @Transactional(readOnly = true)
    public PurchaseEligibilityResponse checkPurchaseEligibility(Long gameId) {
        User currentUser = getCurrentUser();
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        BalanceResponse userBalance = balanceService.getBalance();
        boolean sufficientFunds = balanceService.hasSufficientFunds(game.getPrice());
        
        // Check eligibility without exceptions
        boolean isEligible = isPurchaseEligible(currentUser, game);
        String message = isEligible ? "Purchase eligible" : getIneligibilityReason(currentUser, game);
        
        return purchaseMapper.toPurchaseEligibilityResponse(
                isEligible,
                message,
                game.getId(),
                game.getTitle(),
                game.getPrice(),
                userBalance,
                sufficientFunds
        );
    }
    
    private boolean isPurchaseEligible(User user, Game game) {
        return !purchaseRepository.existsByUserIdAndGameId(user.getId(), game.getId()) &&
               !game.getAuthor().getId().equals(user.getId()) &&
               balanceService.hasSufficientFunds(game.getPrice());
    }
    
    private String getIneligibilityReason(User user, Game game) {
        if (purchaseRepository.existsByUserIdAndGameId(user.getId(), game.getId())) {
            return "You already own this game: " + game.getTitle();
        }
        if (game.getAuthor().getId().equals(user.getId())) {
            return "You cannot purchase your own game: " + game.getTitle();
        }
        if (!balanceService.hasSufficientFunds(game.getPrice())) {
            return String.format("Insufficient funds. Game costs $%.2f but you only have $%.2f", 
                    game.getPrice(), balanceService.getBalance().balance());
        }
        return "Purchase not eligible";
    }

    /**
     * Check if user can purchase multiple games (Used by CartService)
     * @param gameIds list of game IDs to check
     * @return true if all games can be purchased
     */
    @Transactional(readOnly = true)
    public boolean canPurchaseGames(List<Long> gameIds) {
        User currentUser = getCurrentUser();
        List<Game> games = gameRepository.findAllById(gameIds);
        
        if (games.size() != gameIds.size()) {
            return false; // Some games don't exist
        }
        
        // Check each game eligibility
        for (Game game : games) {
            if (!isPurchaseEligible(currentUser, game)) {
                return false;
            }
        }
        
        // Check total cost
        BigDecimal totalCost = games.stream()
                .map(Game::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        return balanceService.hasSufficientFunds(totalCost);
    }

    // ============= PURCHASE HISTORY =============
    
    @Transactional(readOnly = true)
    public List<PurchaseResponse> getMyPurchaseHistory() {
        User currentUser = getCurrentUser();
        return purchaseMapper.toPurchaseResponseList(
                purchaseRepository.findByUserOrderByPurchasedAtDesc(currentUser));
    }
    
    @Transactional(readOnly = true)
    public Page<PurchaseResponse> getMyPurchaseHistoryPaged(Pageable pageable) {
        User currentUser = getCurrentUser();
        return purchaseRepository.findByUser(currentUser, pageable)
                .map(purchaseMapper::toPurchaseResponse);
    }
    
    @Transactional(readOnly = true)
    public List<PurchaseResponse> getPurchasesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        User currentUser = getCurrentUser();
        return purchaseMapper.toPurchaseResponseList(
                purchaseRepository.findByUserAndDateRange(currentUser, startDate, endDate));
    }
    
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public List<PurchaseResponse> getUserPurchaseHistory(Long userId) {
        return purchaseMapper.toPurchaseResponseList(
                purchaseRepository.findByUserIdOrderByPurchasedAtDesc(userId));
    }
    
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public List<PurchaseResponse> getGamePurchases(Long gameId) {
        return purchaseMapper.toPurchaseResponseList(
                purchaseRepository.findByGameIdOrderByPurchasedAtDesc(gameId));
    }
    
    // ============= STATISTICS =============
    
    @Transactional(readOnly = true)
    public PurchaseStatsResponse getMyPurchaseStats() {
        User currentUser = getCurrentUser();
        List<PurchaseHistory> purchases = purchaseRepository.findByUserOrderByPurchasedAtDesc(currentUser);
        
        if (purchases.isEmpty()) {
            return purchaseMapper.toPurchaseStatsResponse(
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null,
                    null
            );
        }
        
        BigDecimal totalSpent = purchaseRepository.calculateTotalSpentByUser(currentUser)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal avgPrice = totalSpent.divide(
                BigDecimal.valueOf(purchases.size()), 2, RoundingMode.HALF_UP);
        
        LocalDateTime firstPurchase = purchases.getLast().getPurchasedAt();
        LocalDateTime lastPurchase = purchases.getFirst().getPurchasedAt();
        
        return purchaseMapper.toPurchaseStatsResponse(
                purchases.size(),
                totalSpent,
                avgPrice,
                firstPurchase,
                lastPurchase
        );
    }
    
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public List<PurchaseResponse> getMySales() {
        User developer = getCurrentUser();
        return purchaseMapper.toPurchaseResponseList(
                purchaseRepository.findSalesByDeveloperId(developer.getId()));
    }
    
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public BigDecimal getMyTotalRevenue() {
        User developer = getCurrentUser();
        return purchaseRepository.calculateTotalRevenueForDeveloper(developer.getId())
                .orElse(BigDecimal.ZERO);
    }
    
    // ============= LIBRARY OPERATIONS =============
    
    @Transactional(readOnly = true)
    public List<Response> getMyPurchases() {
        User currentUser = getCurrentUser();
        return purchaseRepository.findGamesByUser(currentUser).stream()
                .map(gameMapStruct::toDto)
                .collect(Collectors.toList());
    }


    /**
     * Get games owned by user from a list of game IDs (Used by CartService)
     * @param gameIds list of game IDs to check
     * @return list of game IDs that the user owns
     */
    @Transactional(readOnly = true)
    public List<Long> getOwnedGames(List<Long> gameIds) {
        User currentUser = getCurrentUser();
        return gameIds.stream()
                .filter(gameId -> purchaseRepository.existsByUserIdAndGameId(currentUser.getId(), gameId))
                .collect(Collectors.toList());
    }
}
