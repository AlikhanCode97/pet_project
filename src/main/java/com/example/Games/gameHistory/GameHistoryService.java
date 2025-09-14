package com.example.Games.gameHistory;

import com.example.Games.config.common.service.validation.ValidationService;
import com.example.Games.game.Game;
import com.example.Games.gameHistory.dto.DeveloperActivityResponse;
import com.example.Games.gameHistory.dto.FieldChange;
import com.example.Games.gameHistory.dto.GameHistoryResponse;
import com.example.Games.gameHistory.dto.GameTimelineResponse;
import com.example.Games.user.auth.User;
import com.example.Games.user.auth.UserRepository;
import com.example.Games.config.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ValidationService validationService;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public void recordGameCreation(Game game) {
        validationService.validateGame(game);
        User developer = getCurrentUser();
        
        GameHistory history = gameHistoryMapper.createAction(game, developer);
        historyRepository.save(history);
        
        log.info("Recorded game creation: '{}' by developer '{}'", 
                game.getTitle(), developer.getUsername());
    }

    @Transactional
    public void recordGameUpdate(Game game, String field, String oldValue, String newValue) {
        recordGameUpdates(game, List.of(FieldChange.of(field, oldValue, newValue)));
    }

    @Transactional
    public void recordGameUpdates(Game game, List<FieldChange> changes) {
        if (changes == null || changes.isEmpty()) {
            log.debug("No changes to record for game: {}", game != null ? game.getTitle() : "null");
            return;
        }

        validationService.validateGame(game);

        List<FieldChange> actualChanges = changes.stream()
                .filter(FieldChange::hasActualChange)
                .toList();
                
        if (actualChanges.isEmpty()) {
            log.debug("No actual changes detected for game: {}", game.getTitle());
            return;
        }

        User developer = getCurrentUser();
        
        List<GameHistory> historyRecords = actualChanges.stream()
                .map(change -> gameHistoryMapper.updateAction(game, developer, change))
                .collect(Collectors.toList());

        historyRepository.saveAll(historyRecords);
        
        log.info("Recorded {} game updates for '{}' by developer '{}'", 
                actualChanges.size(), game.getTitle(), developer.getUsername());
    }

    @Transactional
    public void recordGameDeletion(Game game) {
        validationService.validateGame(game);
        User developer = getCurrentUser();
        
        GameHistory history = gameHistoryMapper.deleteAction(game, developer);
        historyRepository.save(history);
        
        log.info("Recorded game deletion: '{}' by developer '{}'", 
                game.getTitle(), developer.getUsername());
    }

    @Transactional
    public void recordGamePurchase(Game game, User purchaser, BigDecimal purchasePrice) {
        validationService.validateGame(game);
        validationService.validateUser(purchaser);
        validationService.validatePurchasePrice(purchasePrice);
        
        GameHistory history = gameHistoryMapper.purchaseAction(game, purchaser, purchasePrice);
        historyRepository.save(history);
        
        log.info("Recorded game purchase: '{}' purchased by '{}' for ${}", 
                game.getTitle(), purchaser.getUsername(), purchasePrice);
    }

    @Transactional
    public void recordGamePurchases(List<Game> games, User purchaser, List<BigDecimal> purchasePrices) {
        if (games == null || games.isEmpty()) {
            log.debug("No games to record purchases for");
            return;
        }

        if (purchasePrices == null || purchasePrices.size() != games.size()) {
            throw new IllegalArgumentException("Purchase prices list must match games list size");
        }

        validationService.validateUser(purchaser);

        for (int i = 0; i < games.size(); i++) {
            validationService.validateGame(games.get(i));
            validationService.validatePurchasePrice(purchasePrices.get(i));
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
        if (games == null || games.isEmpty()) {
            return;
        }

        List<BigDecimal> prices = games.stream()
                .map(Game::getPrice)
                .collect(Collectors.toList());

        recordGamePurchases(games, purchaser, prices);
    }

    @Transactional(readOnly = true)
    public DeveloperActivityResponse getDeveloperActivity(Long developerId) {
        validationService.validateUserId(developerId);
        
        User developer = userRepository.findById(developerId)
                .orElseThrow(() -> new ResourceNotFoundException("Developer not found"));

        long totalGamesCreated = historyRepository.countByUserAndActionType(developerId, ActionType.CREATE);
        long totalChanges = historyRepository.countByUserId(developerId); // Optimized count query
        
        LocalDateTime firstActivity = historyRepository.findFirstActivityByUser(developerId);
        LocalDateTime lastActivity = historyRepository.findLastActivityByUser(developerId);
        
        Pageable recentLimit = PageRequest.of(0, 10);
        List<GameHistory> recentHistory = historyRepository.findRecentActivityByUser(developerId, recentLimit);
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
    public Page<GameHistoryResponse> getGameHistory(Long gameId, int page, int size) {
        validationService.validateGameId(gameId);
        validationService.validatePagination(page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<GameHistory> historyPage = historyRepository.findByGameId(gameId, pageable);
        
        return historyPage.map(gameHistoryMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<GameHistoryResponse> getDeveloperHistory(Long developerId, int page, int size) {
        validationService.validateUserId(developerId);
        validationService.validatePagination(page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<GameHistory> historyPage = historyRepository.findByChangedBy_Id(developerId, pageable);
        
        return historyPage.map(gameHistoryMapper::toDto);
    }

    @Transactional(readOnly = true)
    public DeveloperActivityResponse getMyActivity() {
        User currentUser = getCurrentUser();
        return getDeveloperActivity(currentUser.getId());
    }
}
