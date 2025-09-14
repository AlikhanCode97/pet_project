package com.example.Games.user.auth;

import com.example.Games.config.security.CustomUserDetails;
import com.example.Games.user.auth.dto.TokenResponse;
import com.example.Games.user.auth.dto.TokenResponse.UserInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "expiresIn", source = "expiresIn")
    @Mapping(target = "issuedAt", expression = "java(Instant.now())")
    @Mapping(target = "user", expression = "java(toUserInfo(userDetails))")
    TokenResponse toTokenResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            CustomUserDetails userDetails);


    @Mapping(target = "id", source = "userId")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "role", source = "roleName")
    UserInfo toUserInfo(CustomUserDetails userDetails);
}
