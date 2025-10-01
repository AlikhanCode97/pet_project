package com.example.Games.config.exception.auth;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String roleName) {
        super("Role not found: " + roleName);
    }
}
