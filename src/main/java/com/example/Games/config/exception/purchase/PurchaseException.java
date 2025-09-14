package com.example.Games.config.exception.purchase;


public class PurchaseException extends RuntimeException {
    
    public PurchaseException(String message) {
        super(message);
    }

    public static PurchaseException alreadyOwned(String gameTitle) {
        return new PurchaseException("You already own the game: " + gameTitle);
    }

    public static PurchaseException selfPurchase(String gameTitle) {
        return new PurchaseException("You cannot purchase your own game: " + gameTitle);
    }

    public static PurchaseException insufficientFunds(String gameTitle, String amount, String balance) {
        return new PurchaseException(
            String.format("Insufficient funds to purchase '%s'. Game costs %s but you only have %s", 
                gameTitle, amount, balance)
        );
    }
}
