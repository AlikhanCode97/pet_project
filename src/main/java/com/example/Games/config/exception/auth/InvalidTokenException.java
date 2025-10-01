package com.example.Games.config.exception.auth;

public class InvalidTokenException extends RuntimeException {
    
    private final TokenErrorType errorType;
    
    public InvalidTokenException(TokenErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
    
    public TokenErrorType getErrorType() {
        return errorType;
    }
    
    public enum TokenErrorType {
        EXPIRED("Token has expired"),
        MALFORMED("Token is malformed or invalid"),
        WRONG_TYPE("Wrong token type used"),
        BLACKLISTED("Token has been revoked"),
        MISSING("Token is missing");
        
        private final String description;
        
        TokenErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
