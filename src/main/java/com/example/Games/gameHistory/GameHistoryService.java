package com.example.Games.gameHistory;

import com.example.Games.config.common.service.UserContextService;
import com.example.Games.config.exception.auth.UserNotFoundException;
import com.example.Games.config.exception.game.GameNotFoundException;
import com.example.Games.config.exception.gameHistory.GameHistoryException;
import com.example.Games.game.Game;
import com.example.Games.game.GameRepository;
import com.example.Games.gameHistory.dto.DeveloperActivityResponse;
import com.example.Games.gameHistory.dto.FieldChange;
import com.example.Games.gameHistory.dto.GameHistoryResponse;
import com.example.Games.user.auth.User;
import com.example.Games.user.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameHistoryService {

    private final GameHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final GameHistoryMapStruct gameHistoryMapper;
    private final UserContextService userContextService;
    private final GameRepository gameRepository;

    private User getCurrentUser() {
        return userContextService.getAuthorizedUser();
    }

    @Transactional
    public void recordGameCreation(Game game, User currentUser) {
        GameHistory history = gameHistoryMapper.createAction(game, currentUser);
        historyRepository.save(history);
        
        log.info("Recorded game creation: '{}' by developer '{}'", 
                game.getTitle(), currentUser.getUsername());
    }

    @Transactional
    public void recordGameUpdates(Game game, List<FieldChange> changes , User currentUser) {
        List<FieldChange> actualChanges = changes.stream()
                .filter(FieldChange::hasActualChange)
                .toList();

        List<GameHistory> historyRecords = actualChanges.stream()
                .map(change -> gameHistoryMapper.updateAction(game, currentUser, change))
                .collect(Collectors.toList());

        historyRepository.saveAll(historyRecords);
        log.info("Recorded {} game updates for '{}' by developer '{}'", 
                actualChanges.size(), game.getTitle(), currentUser.getUsername());
    }

    @Transactional
    public void recordGameDeletion(Game game, User currentUser) {

        GameHistory history = gameHistoryMapper.deleteAction(game, currentUser);
        historyRepository.save(history);
        
        log.info("Recorded game deletion: '{}' by developer '{}'", 
                game.getTitle(), currentUser.getUsername());
    }

    @Transactional
    public void recordGamePurchase(Game game, User purchaser, BigDecimal purchasePrice) {

        GameHistory history = gameHistoryMapper.purchaseAction(game, purchaser, purchasePrice);
        historyRepository.save(history);
        
        log.info("Recorded game purchase: '{}' purchased by '{}' for ${}", 
                game.getTitle(), purchaser.getUsername(), purchasePrice);
    }

    @Transactional
    public void recordGamePurchases(List<Game> games, User purchaser, List<BigDecimal> purchasePrices) {
        if (purchasePrices == null || purchasePrices.size() != games.size()) {
            throw GameHistoryException.purchasePriceMismatch(
                    games.size(),
                    purchasePrices == null ? 0 : purchasePrices.size()
            );
        }

        List<GameHistory> historyRecords = new ArrayList<>();

        for (int i = 0; i < games.size(); i++) {
            Game game = games.get(i);
            BigDecimal price = purchasePrices.get(i);
            GameHistory history = gameHistoryMapper.purchaseAction(game, purchaser, price);
            historyRecords.add(history);
        }

        historyRepository.saveAll(historyRecords);
        log.info("Recorded {} game purchases for user '{}'", 
                games.size(), purchaser.getUsername());
    }

    @Transactional
    public void recordGamePurchases(List<Game> games, User purchaser) {
        List<BigDecimal> prices = games.stream()
                .map(Game::getPrice)
                .collect(Collectors.toList());

        recordGamePurchases(games, purchaser, prices);
    }

    @Transactional(readOnly = true)
    public DeveloperActivityResponse getMyDeveloperActivity() {
        User developer = getCurrentUser();
        Long developerId = developer.getId();

        long totalGamesCreated = historyRepository.countByUserAndActionType(developerId, ActionType.CREATE);
        long totalChanges = historyRepository.countByUserId(developerId);

        LocalDateTime firstActivity = historyRepository.findFirstActivityByUser(developerId);
        LocalDateTime lastActivity = historyRepository.findLastActivityByUser(developerId);

        List<GameHistory> recentHistory = historyRepository.findTop5ByChangedByIdOrderByChangedAtDesc(developerId);
        List<GameHistoryResponse> recentActivity = gameHistoryMapper.toDtoList(recentHistory);

        return gameHistoryMapper.createDeveloperActivityResponse(
                developer,
                totalGamesCreated,
                totalChanges,
                recentActivity,
                firstActivity,
                lastActivity
        );
    }

    @Transactional(readOnly = true)
    public DeveloperActivityResponse getDeveloperActivity(Long developerId) {
        User developer = userContextService.getUserById(developerId);

        long totalGamesCreated = historyRepository.countByUserAndActionType(developerId, ActionType.CREATE);
        long totalChanges = historyRepository.countByUserId(developerId);
        
        LocalDateTime firstActivity = historyRepository.findFirstActivityByUser(developerId);
        LocalDateTime lastActivity = historyRepository.findLastActivityByUser(developerId);

        List<GameHistory> recentHistory = historyRepository.findTop5ByChangedByIdOrderByChangedAtDesc(developerId);
        List<GameHistoryResponse> recentActivity = gameHistoryMapper.toDtoList(recentHistory);

        return gameHistoryMapper.createDeveloperActivityResponse(
                developer,
                totalGamesCreated,
                totalChanges,
                recentActivity,
                firstActivity,
                lastActivity
        );
    }

    @Transactional(readOnly = true)
    public Page<GameHistoryResponse> getMyGameHistory(Long gameId, int page, int size) {
        User developer = getCurrentUser();

        Game myGame = gameRepository.findByIdWithAuthor(gameId)
                .orElseThrow(() -> GameNotFoundException.byId(gameId));
        if (!myGame.getAuthor().getId().equals(developer.getId())) {
            throw GameHistoryException.unauthorizedAccess(gameId, developer.getUsername());
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<GameHistory> historyPage = historyRepository.findByGameIdWithRelations(gameId, pageable);
        return historyPage.map(gameHistoryMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<GameHistoryResponse> getGameHistory(Long gameId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GameHistory> historyPage = historyRepository.findByGameIdWithRelations(gameId, pageable);
        return historyPage.map(gameHistoryMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<GameHistoryResponse> getDeveloperHistory(Long developerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GameHistory> historyPage = historyRepository.findByChangedByIdWithRelations(developerId, pageable);
        return historyPage.map(gameHistoryMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<GameHistoryResponse> getMyDeveloperHistory(int page, int size) {
        User developer = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<GameHistory> historyPage = historyRepository.findByChangedByIdWithRelations(developer.getId(), pageable);
        return historyPage.map(gameHistoryMapper::toDto);
    }
}
