package com.example.Games.user.balance.transaction;

import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.user.balance.transaction.dto.BalanceTransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/balance/transactions")
@RequiredArgsConstructor
public class BalanceTransactionController {

    private final BalanceTransactionService transactionService;
    private final ResponseMapStruct responseMapper;

    @GetMapping("/user/{userId}")
    @PreAuthorize("@authorizationUtils.isAdmin()")
    public ResponseEntity<ApiResponse<List<BalanceTransactionDTO>>> getUserTransactions(@PathVariable Long userId) {
        List<BalanceTransactionDTO> transactions = transactionService.getUserTransactions(userId);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("User transaction history retrieved", transactions)
        );
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BalanceTransactionDTO>>> getMyTransactions() {
        List<BalanceTransactionDTO> transactions = transactionService.getMyTransactions();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("My transaction history retrieved", transactions)
        );
    }
}