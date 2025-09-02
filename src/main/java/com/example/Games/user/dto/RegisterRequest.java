package com.example.Games.user.dto;

import com.example.Games.user.role.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 20)
        String username,

        @Email
        String email,

        @NotBlank @Size(min = 8)
        String password,

        RoleType roleType
) {}
