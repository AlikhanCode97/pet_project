package com.example.Games.user.balance;

import com.example.Games.user.auth.User;
import com.example.Games.user.balance.dto.*;
import com.example.Games.config.common.service.UserContextService;
import com.example.Games.user.balance.transaction.BalanceTransaction;
import com.example.Games.user.balance.transaction.BalanceTransactionRepository;
import com.example.Games.user.balance.transaction.OperationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceMapStruct balanceMapper;
    private final UserContextService userContextService;
    private final BalanceTransactionRepository transactionRepository;

    private User getCurrentUser() {
        return userContextService.getCurrentUser();
    }

    @Transactional
    public BalanceResponse getBalance() {
        User user = getCurrentUser();
        BigDecimal balance = getOrCreateBalance(user).getAmount();
        return balanceMapper.toBalanceResponse(balance);
    }

    @Transactional
    public BigDecimal getRawBalance() {
        User user = getCurrentUser();
        return getOrCreateBalance(user).getAmount();
    }

    @Transactional
    public BalanceResponse getUserBalance(Long userId) {
        User user = userContextService.getUserById(userId);
        BigDecimal balance = getOrCreateBalance(user).getAmount();
        return balanceMapper.toUserBalanceResponse(userId, balance);
    }

    @Transactional
    public BalanceResponse deposit(DepositRequest request) {
        User user = getCurrentUser();
        Balance balance = getOrCreateBalance(user);
        
        BigDecimal balanceBefore = balance.getAmount();
        balance.deposit(request.amount());
        balanceRepository.save(balance);

        BalanceTransaction transaction = balance.createTransaction(
                OperationType.DEPOSIT, 
                request.amount(), 
                balanceBefore
        );
        transactionRepository.save(transaction);
        
        log.info("Deposit successful - User: {}, Amount: ${}, New Balance: ${}", 
                user.getUsername(), request.amount(), balance.getAmount());
        
        return balanceMapper.toBalanceOperationResponse(
                request.amount(), 
                balance.getAmount(), 
                "DEPOSIT"
        );
    }

    @Transactional
    public BalanceResponse depositForUser(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Admin deposit amount must be positive");
        }
        
        User user = userContextService.getUserById(userId);
        
        Balance balance = getOrCreateBalance(user);
        BigDecimal balanceBefore = balance.getAmount();
        balance.deposit(amount);
        balanceRepository.save(balance);
        
        BalanceTransaction transaction = balance.createTransaction(
                OperationType.ADMIN_DEPOSIT, 
                amount, 
                balanceBefore
        );
        transactionRepository.save(transaction);
        
        log.info("Admin deposit successful - User: {}, Amount: ${}, New Balance: ${}", 
                user.getUsername(), amount, balance.getAmount());
        
        return balanceMapper.toBalanceOperationResponse(
                amount, 
                balance.getAmount(), 
                "ADMIN_DEPOSIT"
        );
    }

    @Transactional
    public BalanceResponse withdraw(WithdrawRequest request) {
        User user = getCurrentUser();
        Balance balance = getOrCreateBalance(user);
        
        // Business rule: sufficient funds check
        if (!balance.hasSufficientFunds(request.amount())) {
            throw new IllegalArgumentException(
                String.format("Insufficient funds. Current: $%.2f, Requested: $%.2f", 
                    balance.getAmount(), request.amount())
            );
        }
        
        BigDecimal balanceBefore = balance.getAmount();
        balance.withdraw(request.amount());
        balanceRepository.save(balance);

        BalanceTransaction transaction = balance.createTransaction(
                OperationType.WITHDRAWAL, 
                request.amount(), 
                balanceBefore
        );
        transactionRepository.save(transaction);

        log.info("Withdrawal successful - User: {}, Amount: ${}, New Balance: ${}", 
                user.getUsername(), request.amount(), balance.getAmount());
        
        return balanceMapper.toBalanceOperationResponse(
                request.amount(), 
                balance.getAmount(), 
                "WITHDRAWAL"
        );
    }

    @Transactional(readOnly = true)
    public boolean hasSufficientFunds(BigDecimal amount) {
        User user = getCurrentUser();
        Balance balance = getOrCreateBalance(user);
        return balance.hasSufficientFunds(amount);
    }

    @Transactional
    public BalanceTransaction createPurchaseTransaction(BigDecimal amount) {
        User user = getCurrentUser();
        Balance balance = getOrCreateBalance(user);

        if (!balance.hasSufficientFunds(amount)) {
            throw new IllegalArgumentException(
                String.format("Insufficient funds. Current: $%.2f, Requested: $%.2f", 
                    balance.getAmount(), amount)
            );
        }
        
        BigDecimal balanceBefore = balance.getAmount();
        balance.withdraw(amount);
        balanceRepository.save(balance);
        
        BalanceTransaction transaction = balance.createTransaction(
                OperationType.PURCHASE,
                amount,
                balanceBefore
        );
        return transactionRepository.save(transaction);
    }

    private Balance getOrCreateBalance(User user) {
        return balanceRepository.findByUser(user)
                .orElseGet(() -> {
                    log.info("Creating new balance for user: {}", user.getUsername());
                    Balance newBalance = Balance.builder()
                            .user(user)
                            .amount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                            .build();
                    return balanceRepository.save(newBalance);
                });
    }
}
