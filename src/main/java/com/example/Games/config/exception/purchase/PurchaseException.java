package com.example.Games.config.exception.purchase;


import java.util.List;

public class PurchaseException extends RuntimeException {
    
    public PurchaseException(String message) {
        super(message);
    }
    public static PurchaseException selfPurchase(String gameTitle) {
        return new PurchaseException("You cannot purchase your own game: " + gameTitle);
    }
    public static PurchaseException selfPurchases(List<String> gameTitles) {
        return new PurchaseException("You cannot purchase your own game: " + gameTitles);
    }
}
