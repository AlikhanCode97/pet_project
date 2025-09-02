package com.example.Games.user;

import com.example.Games.user.balance.Balance;
import com.example.Games.user.balance.BalanceRepository;
import com.example.Games.user.dto.AuthRequest;
import com.example.Games.user.dto.AuthResponse;
import com.example.Games.user.dto.RegisterRequest;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleRepository;
import com.example.Games.config.security.CustomUserDetails;
import com.example.Games.config.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final BalanceRepository balanceRepository;

    public AuthResponse register(RegisterRequest request) {

        Role defaultRole = roleRepository.findByName(request.roleType())
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(defaultRole)
                .build();

        Balance balance = Balance.builder()
                .user(user)
                .amount(BigDecimal.ZERO)
                .build();
        balanceRepository.save(balance);

        userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userDetails.getUserId());

        String jwt = jwtService.generateToken(extraClaims, userDetails);
        return new AuthResponse(jwt);
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // You can create a UserDetails object manually or reuse your `CustomUserDetails`
        UserDetails userDetails = new CustomUserDetails(user);
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());

        String jwt = jwtService.generateToken(extraClaims, userDetails);
        return new AuthResponse(jwt);
    }
}
