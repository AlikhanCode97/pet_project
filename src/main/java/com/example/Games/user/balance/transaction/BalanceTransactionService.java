package com.example.Games.user.balance.transaction;

import com.example.Games.config.exception.balance.BalanceAlreadyExistsException;
import com.example.Games.user.balance.Balance;
import com.example.Games.user.balance.BalanceRepository;
import com.example.Games.user.auth.User;
import com.example.Games.config.common.service.UserContextService;
import com.example.Games.user.balance.dto.BalanceResponse;
import com.example.Games.user.balance.transaction.dto.BalanceTransactionDTO;
import com.example.Games.config.exception.balance.BalanceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceTransactionService {

    private final BalanceTransactionRepository transactionRepository;
    private final BalanceRepository balanceRepository;
    private final UserContextService userContextService;
    private final BalanceTransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public List<BalanceTransactionDTO> getUserTransactions(Long userId) {
        User user = userContextService.getUserById(userId);
        Balance balance = balanceRepository.findByUser(user)
                .orElseThrow(() -> new BalanceNotFoundException(user.getUsername()));

        return transactionMapper.toDtoList(
                transactionRepository.findAllByBalanceOrderByTimestampDesc(balance)
        );
    }

    @Transactional(readOnly = true)
    public List<BalanceTransactionDTO> getMyTransactions() {
        User currentUser = userContextService.getAuthorizedUser();
        Balance balance = balanceRepository.findByUser(currentUser)
                .orElseThrow(() -> new BalanceNotFoundException(currentUser.getUsername()));

        return transactionMapper.toDtoList(
                transactionRepository.findAllByBalanceOrderByTimestampDesc(balance)
        );
    }

}
