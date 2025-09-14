package com.example.Games.user.balance.transaction;

import com.example.Games.user.balance.Balance;
import com.example.Games.user.balance.BalanceRepository;
import com.example.Games.user.auth.User;
import com.example.Games.config.common.service.UserContextService;
import com.example.Games.user.balance.transaction.dto.BalanceTransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceTransactionService {

    private final BalanceTransactionRepository transactionRepository;
    private final BalanceRepository balanceRepository;
    private final UserContextService userContextService;
    private final BalanceTransactionMapper transactionMapper;

    // Get current user's transactions
    @Transactional(readOnly = true)
    public List<BalanceTransactionDTO> getMyTransactions() {
        User user = userContextService.getCurrentUser();
        Balance balance = balanceRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Balance not found for user " + user.getId()));

        return transactionMapper.toDtoList(
                transactionRepository.findAllByBalanceOrderByTimestampDesc(balance)
        );
    }

    // Get transactions for a specific user (admin only)
    @Transactional(readOnly = true)
    public List<BalanceTransactionDTO> getUserTransactions(Long userId) {
        User user = userContextService.getUserById(userId);
        Balance balance = balanceRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Balance not found for user " + userId));

        return transactionMapper.toDtoList(
                transactionRepository.findAllByBalanceOrderByTimestampDesc(balance)
        );
    }
}
