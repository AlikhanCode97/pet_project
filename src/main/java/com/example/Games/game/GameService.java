package com.example.Games.game;

import com.example.Games.category.Category;
import com.example.Games.category.CategoryRepository;
import com.example.Games.config.exception.ResourceNotFoundException;
import com.example.Games.game.dto.CreateRequest;
import com.example.Games.game.dto.Response;
import com.example.Games.game.dto.UpdateRequest;
import com.example.Games.history.GameHistory;
import com.example.Games.history.GameHistoryRepository;
import com.example.Games.rating.GameRating;
import com.example.Games.rating.GameRatingRepository;
import com.example.Games.rating.RatingLabel;
import com.example.Games.rating.RatingUtils;
import com.example.Games.user.User;
import com.example.Games.user.userLibrary.UserLibrary;
import com.example.Games.user.userLibrary.UserLibraryRepository;
import com.example.Games.user.UserRepository;
import com.example.Games.user.balance.Balance;
import com.example.Games.user.balance.BalanceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final CategoryRepository categoryRepository;
    private final GameMapStruct gameMapStruct;
    private final GameHistoryRepository gameHistoryRepository;
    private final UserRepository userRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final GameRatingRepository gameRatingRepository;
    private final BalanceRepository balanceRepository;

    private void logChange(Game game, String field, Object oldVal, Object newVal, User user) {
        if (oldVal != null && newVal != null && !oldVal.equals(newVal)) {
            GameHistory history = GameHistory.builder()
                    .gameId(game.getId())
                    .fieldChanged(field)
                    .oldValue(oldVal.toString())
                    .newValue(newVal.toString())
                    .changedBy(user)
                    .build();
            gameHistoryRepository.save(history);
        }
    }

    @Transactional
    public void buyGame(Long gameId) {
        User user = getCurrentUser();
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        if (userLibraryRepository.existsByUserIdAndGameId(user.getId(), game.getId())) {
            throw new IllegalStateException("You already own this game.");
        }

        if (user.getUsername().equals(game.getAuthor())) {
            throw new IllegalStateException("You cannot buy your own game.");
        }

        Balance balance = balanceRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Balance not found"));

        balance.withdraw(game.getPrice()); // will throw if not enough
        balanceRepository.save(balance);

        UserLibrary entry = UserLibrary.of(user, game);
        userLibraryRepository.save(entry);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public RatingLabel getAllTimeRatingLabel(Long gameId) {
        var ratings = gameRatingRepository.findByGameId(gameId);
        long total = ratings.size();
        long recommended = ratings.stream().filter(GameRating::isRecommended).count();
        return RatingUtils.computeRatingLabel(total, recommended);
    }

    public RatingLabel getRecentRatingLabel(Long gameId) {
        var ratings = gameRatingRepository.findByGameId(gameId).stream()
                .filter(r -> r.getRatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .toList();
        long total = ratings.size();
        long recommended = ratings.stream().filter(GameRating::isRecommended).count();
        return RatingUtils.computeRatingLabel(total, recommended);
    }



    // ✅ Create
    public Response createGame(CreateRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Game game = gameMapStruct.toEntity(request, category, username);
        Game savedGame = gameRepository.save(game);

        UserLibrary libraryEntry = UserLibrary.of(user, savedGame);
        userLibraryRepository.save(libraryEntry);

        return gameMapStruct.toDto(savedGame);
    }

    // ✅ Update
    public Response updateGame(Long id, UpdateRequest request) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));


        if (request.title() == null && request.price() == null &&
                request.categoryId() == null) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }
        User user = getCurrentUser();

        if (request.title() != null) {
            logChange(game, "title", game.getTitle(), request.title(), user);
            game.updateTitle(request.title());
        }
         if (request.price() != null) {
            logChange(game, "price", game.getPrice(), request.price(), user);
            game.updatePrice(request.price());
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            logChange(game, "category", game.getCategory().getId(), category.getId(), user);
            game.updateCategory(category);
        }

        return gameMapStruct.toDto(gameRepository.save(game));
    }

    // ✅ Read all
    public List<Response> getAllGames() {
        return gameRepository.findAll()
                .stream()
                .map(gameMapStruct::toDto)
                .toList();
    }

    // ✅ Read by ID
    public Response getGameById(String title) {
        Game game = gameRepository.findByTitle(title)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));
        return gameMapStruct.toDto(game);
    }

    // ✅ Delete
    public void deleteGame(Long id) {
        if (!gameRepository.existsById(id)) {
            throw new ResourceNotFoundException("Game not found");
        }
        gameRepository.deleteById(id);
    }

    // ✅ findByTitle (Spring method naming)
    public List<Response> searchByTitle(String title) {
        return gameRepository.findByTitle(title)
                .stream()
                .map(gameMapStruct::toDto)
                .toList();
    }

    // ✅ findGamesInPriceRange (custom JPQL)
    public List<Response> getGamesInPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return gameRepository.findGamesInPriceRange(minPrice, maxPrice)
                .stream()
                .map(gameMapStruct::toDto)
                .toList();
    }

    // ✅ findByCategoryId + Pageable
    public Page<Response> getGamesByCategoryPaged(Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return gameRepository.findByCategoryId(categoryId, pageable)
                .map(gameMapStruct::toDto);
    }

    // ✅ Sorting by price ascending
    public List<Response> getGamesSortedByPrice() {
        return gameRepository.findAllByOrderByPriceAsc()
                .stream()
                .map(gameMapStruct::toDto)
                .toList();
    }

    // ✅ Native query (search by author)
    public List<Response> searchGamesByAuthorNative(String author) {
        return gameRepository.searchByAuthorNative(author)
                .stream()
                .map(gameMapStruct::toDto)
                .toList();
    }

    // ✅ Combined: JPQL + pagination + sorting
    public Page<Response> searchGamesByTitlePaged(String keyword, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(Sort.Order.asc(sortBy)) : Sort.by(Sort.Order.desc(sortBy));
        Pageable pageable = PageRequest.of(page, size, sort);
        return gameRepository.searchByTitle(keyword, pageable)
                .map(gameMapStruct::toDto);
    }

}
