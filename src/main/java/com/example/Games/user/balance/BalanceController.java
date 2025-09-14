package com.example.Games.user.balance;

import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.user.balance.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/balance")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;
    private final ResponseMapStruct responseMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance() {
        BalanceResponse balance = balanceService.getBalance();
        return ResponseEntity.ok(responseMapper.toSuccessResponse(balance));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@authUtils.isDeveloper()")
    public ResponseEntity<ApiResponse<BalanceResponse>> getUserBalance(@PathVariable Long userId) {
        BalanceResponse balance = balanceService.getUserBalance(userId);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("User balance retrieved", balance)
        );
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<BalanceResponse>> deposit(@Valid @RequestBody DepositRequest request) {
        BalanceResponse result = balanceService.deposit(request);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Deposit completed successfully", result)
        );
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<BalanceResponse>> withdraw(@Valid @RequestBody WithdrawRequest request) {
        BalanceResponse result = balanceService.withdraw(request);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Withdrawal completed successfully", result)
        );
    }

    @PostMapping("/admin/deposit/{userId}")
    @PreAuthorize("@authUtils.isDeveloper()")
    public ResponseEntity<ApiResponse<BalanceResponse>> depositForUser(@PathVariable Long userId,
                                                                      @Valid @RequestBody DepositRequest request) {
        BalanceResponse result = balanceService.depositForUser(userId, request.amount());
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Admin deposit completed successfully", result)
        );
    }
}
