package com.example.Games.user.balance;

import com.example.Games.user.auth.User;
import com.example.Games.user.balance.dto.*;
import com.example.Games.config.common.service.UserContextService;
import com.example.Games.user.balance.transaction.BalanceTransaction;
import com.example.Games.user.balance.transaction.BalanceTransactionRepository;
import com.example.Games.user.balance.transaction.OperationType;
import com.example.Games.config.exception.balance.BalanceNotFoundException;
import com.example.Games.config.exception.balance.BalanceAlreadyExistsException;
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
        return userContextService.getAuthorizedUser();
    }

    @Transactional
    public BalanceResponse createBalance() {
        User user = getCurrentUser();

        if (balanceRepository.findByUser(user).isPresent()) {
            throw new BalanceAlreadyExistsException(user.getUsername());
        }
        
        Balance newBalance = Balance.builder()
                .user(user)
                .amount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .build();
        
        Balance savedBalance = balanceRepository.save(newBalance);
        log.info("Balance created for user: {}", user.getUsername());
        
        return balanceMapper.toBalanceResponse(user.getId(), savedBalance.getAmount());
    }

    private Balance getBalance(User user) {
        return balanceRepository.findByUser(user)
                .orElseThrow(() -> new BalanceNotFoundException(user.getUsername()));
    }

    @Transactional(readOnly = true)
    public BalanceResponse getMyBalance() {
        User user = getCurrentUser();
        Balance balance = getBalance(user);
        return balanceMapper.toBalanceResponse(user.getId(), balance.getAmount());
    }


    @Transactional(readOnly = true)
    public BalanceResponse getUserBalance(Long userId) {
        User user = userContextService.getUserById(userId);
        Balance balance = getBalance(user);
        return balanceMapper.toBalanceResponse(userId, balance.getAmount());
    }

    @Transactional
    public void deleteBalance() {
        User user = getCurrentUser();
        Balance balance = getBalance(user);
        balanceRepository.delete(balance);
        log.info("Balance deleted for user: {}", user.getUsername());
    }

    @Transactional
    public BalanceOperationResponse deposit(DepositRequest request) {
        User user = getCurrentUser();
        Balance balance = getBalance(user);
        
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
                balance.getAmount(),
                user.getId(),
                request.amount(),
                OperationType.DEPOSIT
        );
    }

    @Transactional
    public BalanceOperationResponse withdraw(WithdrawRequest request) {
        User user = getCurrentUser();
        Balance balance = getBalance(user);

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
                balance.getAmount(),
                user.getId(),
                request.amount(),
                OperationType.WITHDRAWAL
        );
    }

    @Transactional(readOnly = true)
    public boolean canAfford(BigDecimal amount) {

        User user = getCurrentUser();
        Balance balance = getBalance(user);
        return balance.hasSufficientFunds(amount);

    }

    @Transactional
    public BalanceTransaction createPurchaseTransaction(BigDecimal amount , User currentUser) {
        Balance balance = getBalance(currentUser);
        
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
}
