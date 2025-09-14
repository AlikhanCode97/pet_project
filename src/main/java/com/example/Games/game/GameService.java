package com.example.Games.game;

import com.example.Games.category.Category;
import com.example.Games.category.CategoryRepository;
import com.example.Games.config.common.service.validation.ValidationService;
import com.example.Games.config.exception.ResourceNotFoundException;
import com.example.Games.config.exception.game.GameNotFoundException;
import com.example.Games.config.common.service.UserContextService;
import com.example.Games.game.dto.CreateRequest;
import com.example.Games.game.dto.PagedResponse;
import com.example.Games.game.dto.Response;
import com.example.Games.game.dto.UpdateRequest;
import com.example.Games.gameHistory.GameHistoryService;
import com.example.Games.gameHistory.dto.FieldChange;
import com.example.Games.user.auth.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final CategoryRepository categoryRepository;
    private final GameMapStruct gameMapStruct;
    private final GameHistoryService historyService;
    private final UserContextService userContextService;
    private final ValidationService validationService;

    private User getCurrentUser() {
        return userContextService.getCurrentUser();
    }

    @Transactional
    public Response createGame(CreateRequest request) {
        User currentUser = getCurrentUser();
        
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Game game = gameMapStruct.toEntity(request, category, currentUser);
        Game savedGame = gameRepository.save(game);

        historyService.recordGameCreation(savedGame);

        log.info("Game '{}' created by user '{}'", savedGame.getTitle(), currentUser.getUsername());
        
        return gameMapStruct.toDto(savedGame);
    }

    @Transactional
    public Response updateGame(Long id, UpdateRequest request) {
        validationService.validateGameId(id);
        
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> GameNotFoundException.byId(id));

        User currentUser = getCurrentUser();

        if (!game.getAuthor().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You can only update your own games");
        }

        List<FieldChange> changes = new ArrayList<>();

        if (request.title() != null) {
            String oldTitle = game.getTitle();
            game.updateTitle(request.title());
            changes.add(FieldChange.of("title", oldTitle, request.title()));
        }
        
        if (request.price() != null) {
            validationService.validateAmount(request.price());
            BigDecimal oldPrice = game.getPrice();
            game.updatePrice(request.price());
            changes.add(FieldChange.of("price", oldPrice.toString(), request.price().toString()));
        }
        
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            Long oldCategoryId = game.getCategory().getId();
            game.updateCategory(category);
            changes.add(FieldChange.of("category", oldCategoryId.toString(), category.getId().toString()));
        }

        Game updatedGame = gameRepository.save(game);

        historyService.recordGameUpdates(updatedGame, changes);
        
        log.info("Game '{}' updated by user '{}'", updatedGame.getTitle(), currentUser.getUsername());
        
        return gameMapStruct.toDto(updatedGame);
    }

    @Transactional(readOnly = true)
    public List<Response> getAllGames() {
        return gameRepository.findAll()
                .stream()
                .map(gameMapStruct::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Response getGameById(Long id) {
        validationService.validateGameId(id);
        
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> GameNotFoundException.byId(id));
        return gameMapStruct.toDto(game);
    }

    @Transactional(readOnly = true)
    public Response getGameByTitle(String title) {
        validationService.validateTitle(title);
        
        Game game = gameRepository.findByTitle(title)
                .orElseThrow(() -> GameNotFoundException.byTitle(title));
        return gameMapStruct.toDto(game);
    }

    @Transactional
    public void deleteGame(Long id) {
        validationService.validateGameId(id);
        
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> GameNotFoundException.byId(id));
        
        User currentUser = getCurrentUser();

        if (!game.getAuthor().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You can only delete your own games");
        }
        
        historyService.recordGameDeletion(game);
        gameRepository.deleteById(id);
        log.info("Game with ID {} deleted by user '{}'", id, currentUser.getUsername());
    }

    @Transactional(readOnly = true)
    public List<Response> searchByTitle(String title) {
        validationService.validateTitle(title);
        
        return gameRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(gameMapStruct::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Response> searchGamesByAuthor(String author) {
        validationService.validateUsername(author);
        
        return gameRepository.findByAuthor_Username(author)
                .stream()
                .map(gameMapStruct::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Response> getGamesInPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        validationService.validateAmount(minPrice);
        validationService.validateAmount(maxPrice);
        
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price");
        }
        
        return gameRepository.findGamesInPriceRange(minPrice, maxPrice)
                .stream()
                .map(gameMapStruct::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Response> getGamesSortedByPrice(boolean ascending) {
        List<Game> games = ascending 
                ? gameRepository.findAllByOrderByPriceAsc()
                : gameRepository.findAllByOrderByPriceDesc();
                
        return games.stream()
                .map(gameMapStruct::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse getGamesByCategoryPaged(Long categoryId, int page, int size) {
        validationService.validateId(categoryId, "Category ID");
        validationService.validatePagination(page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("title"));
        Page<Game> gamesPage = gameRepository.findByCategoryId(categoryId, pageable);
        
        List<Response> games = gamesPage.getContent()
                .stream()
                .map(gameMapStruct::toDto)
                .toList();
                
        return new PagedResponse(
                games,
                gamesPage.getNumber(),
                gamesPage.getSize(),
                gamesPage.getTotalElements(),
                gamesPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<Response> getMyGames() {
        User currentUser = getCurrentUser();
        return gameRepository.findByAuthor(currentUser)
                .stream()
                .map(gameMapStruct::toDto)
                .toList();
    }
}
