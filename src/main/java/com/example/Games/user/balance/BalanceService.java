package com.example.Games.user.balance;

import com.example.Games.user.User;
import com.example.Games.user.UserRepository;
import com.example.Games.user.balance.dto.DepositRequest;
import com.example.Games.user.balance.dto.WithdrawRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final UserRepository userRepository;
    private final BalanceRepository balanceRepository;

    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public BigDecimal getBalance() {
        User user = getCurrentUser();
        return balanceRepository.findByUser(user)
                .map(Balance::getAmount)
                .orElseThrow(() -> new RuntimeException("Balance not found"));
    }

    @Transactional
    public BigDecimal deposit(DepositRequest request) {
        User user = getCurrentUser();
        Balance balance = balanceRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Balance not found"));
        balance.deposit(request.amount());
        return balanceRepository.save(balance).getAmount();
    }

    @Transactional
    public BigDecimal withdraw(WithdrawRequest request) {
        User user = getCurrentUser();
        Balance balance = balanceRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Balance not found"));
        balance.withdraw(request.amount());
        return balanceRepository.save(balance).getAmount();
    }
}
