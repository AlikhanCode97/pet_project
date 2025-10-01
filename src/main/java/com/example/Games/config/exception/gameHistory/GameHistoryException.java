package com.example.Games.config.exception.gameHistory;

public class GameHistoryException extends RuntimeException {

  public GameHistoryException(String message) {
    super(message);
  }

  public static GameHistoryException purchasePriceMismatch(int gamesCount, int pricesCount) {
    return new GameHistoryException(
            String.format("Purchase prices count (%d) does not match games count (%d)",
                    pricesCount, gamesCount)
    );
  }

  public static GameHistoryException unauthorizedAccess(Long gameId, String username) {
    return new GameHistoryException(
            String.format("User '%s' is not authorized to access history for game with ID: %d",
                    username, gameId)
    );
  }
}