package com.example.Games.purchase;

import com.example.Games.config.common.service.UserContextService;
import com.example.Games.config.exception.game.GameNotFoundException;
import com.example.Games.game.Game;
import com.example.Games.game.GameRepository;
import com.example.Games.game.GameMapStruct;
import com.example.Games.game.dto.Response;
import com.example.Games.gameHistory.GameHistoryService;
import com.example.Games.purchase.dto.PurchaseGamesRequest;
import com.example.Games.purchase.dto.PurchaseResponse;
import com.example.Games.user.auth.User;
import com.example.Games.user.auth.UserRepository;
import com.example.Games.user.balance.BalanceService;
import com.example.Games.user.balance.transaction.BalanceTransaction;
import com.example.Games.config.exception.purchase.GameAlreadyOwnedException;
import com.example.Games.config.exception.purchase.PurchaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final UserContextService userContextService;

    private User getCurrentUser() {
        return userContextService.getAuthorizedUser();
    }

    private void validateGamePurchases(User user, List<Game> games, List<Long> gameIds) {
        if (games.size() != gameIds.size()) {
            throw new GameNotFoundException("One or more games not found");
        }
        List<Long> ownedIds = purchaseRepository.findOwnedGameIds(user.getId(), gameIds);
        if (!ownedIds.isEmpty()) {
            throw new GameAlreadyOwnedException(
                    "You already own games with IDs: " + ownedIds
            );
        }
        List<String> ownGames = games.stream()
                .filter(g -> g.getAuthor().getId().equals(user.getId()))
                .map(Game::getTitle)
                .toList();

        if (!ownGames.isEmpty()) {
            throw PurchaseException.selfPurchases(ownGames);
        }
    }

    @Transactional
    public PurchaseResponse purchaseGame(Long gameId) {
        User currentUser = getCurrentUser();

        Game game = gameRepository.findByIdWithAuthor(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        if (purchaseRepository.existsByUserIdAndGameId(currentUser.getId(), game.getId())) {
            throw new GameAlreadyOwnedException("You already own this game: " + game.getTitle());
        }

        if (game.getAuthor().getId().equals(currentUser.getId())) {
            throw PurchaseException.selfPurchase(game.getTitle());
        }

        BalanceTransaction balanceTransaction = balanceService.createPurchaseTransaction(game.getPrice() , currentUser);

        PurchaseHistory purchase = purchaseMapper.createPurchase(currentUser, game);
        purchaseRepository.save(purchase);
        gameHistoryService.recordGamePurchase(game, currentUser, game.getPrice());

        log.info("Game '{}' purchased by user '{}' for ${} - Purchase ID: {}, Transaction ID: {}", 
                game.getTitle(), currentUser.getUsername(), game.getPrice(), 
                purchase.getId(), balanceTransaction.getId());

        return purchaseMapper.toPurchaseResponse(purchase);
    }

    @Transactional
    public List<PurchaseResponse> purchaseGamesByIds(PurchaseGamesRequest request) {
        User currentUser = getCurrentUser();
        List<Game> games = gameRepository.findAllByIdWithAuthor(request.gameIds());

        validateGamePurchases(currentUser,games,request.gameIds());
        return purchaseGames(games, currentUser);
    }

    @Transactional
    public List<PurchaseResponse> purchaseGames(List<Game> games, User user) {

        BigDecimal totalCost = games.stream()
                .map(Game::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BalanceTransaction balanceTransaction = balanceService.createPurchaseTransaction(totalCost ,user);

        List<PurchaseHistory> purchases = new ArrayList<>();
        for (Game game : games) {
            PurchaseHistory purchase = purchaseMapper.createPurchase(user, game);
            purchases.add(purchase);
        }

        purchases = purchaseRepository.saveAll(purchases);
        gameHistoryService.recordGamePurchases(games, user);

        log.info("Batch purchase completed for user '{}': {} games purchased for ${} - Transaction ID: {}", 
                user.getUsername(), games.size(), totalCost, balanceTransaction.getId());

        return purchases.stream()
                .map(purchaseMapper::toPurchaseResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean canPurchaseGames(List<Long> gameIds) {
        User currentUser = getCurrentUser();
        List<Game> games = gameRepository.findAllByIdWithAuthor(gameIds);

        validateGamePurchases(currentUser, games, gameIds);

        BigDecimal totalCost = games.stream()
                .map(Game::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return balanceService.canAfford(totalCost);
    }


    @Transactional(readOnly = true)
    public List<PurchaseResponse> getMyPurchaseHistory() {
        User currentUser = getCurrentUser();
        return purchaseMapper.toPurchaseResponseList(
                purchaseRepository.findByUserWithGameAndAuthor(currentUser));
    }
    
    @Transactional(readOnly = true)
    public Page<PurchaseResponse> getMyPurchaseHistoryPaged(Pageable pageable) {
        User currentUser = getCurrentUser();
        return purchaseRepository.findByUserWithGameAndAuthor(currentUser, pageable)
                .map(purchaseMapper::toPurchaseResponse);
    }
    
    @Transactional(readOnly = true)
    public List<PurchaseResponse> getUserPurchaseHistory(Long userId) {
        return purchaseMapper.toPurchaseResponseList(
                purchaseRepository.findByUserIdWithGameAndAuthor(userId));
    }
    
    @Transactional(readOnly = true)
    public List<PurchaseResponse> getGamePurchases(Long gameId) {
        return purchaseMapper.toPurchaseResponseList(
                purchaseRepository.findByGameIdWithGameAndAuthor(gameId));
    }
    
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public List<PurchaseResponse> getMySales() {
        User developer = getCurrentUser();
        return purchaseMapper.toPurchaseResponseList(
                purchaseRepository.findSalesByDeveloperIdWithGame(developer.getId()));
    }
    
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public BigDecimal getMyTotalRevenue() {
        User developer = getCurrentUser();
        return purchaseRepository.calculateTotalRevenueForDeveloper(developer.getId())
                .orElse(BigDecimal.ZERO);
    }
}
