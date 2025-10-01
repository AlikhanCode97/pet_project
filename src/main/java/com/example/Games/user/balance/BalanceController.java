package com.example.Games.user.balance;

import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.user.balance.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BalanceResponse>> getMyBalance() {
        BalanceResponse balance = balanceService.getMyBalance();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Balance retrieved", balance)
        );
    }

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BalanceResponse>> createBalance() {
        BalanceResponse balance = balanceService.createBalance();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseMapper.toSuccessResponse("Balance created successfully", balance));
    }
    
    @DeleteMapping("/delete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> deleteBalance() {
        balanceService.deleteBalance();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Balance deleted successfully")
        );
    }

    @PostMapping("/deposit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BalanceOperationResponse>> deposit(@Valid @RequestBody DepositRequest request) {
        BalanceOperationResponse result = balanceService.deposit(request);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Deposit completed successfully", result)
        );
    }

    @PostMapping("/withdraw")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BalanceOperationResponse>> withdraw(@Valid @RequestBody WithdrawRequest request) {
        BalanceOperationResponse result = balanceService.withdraw(request);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Withdrawal completed successfully", result)
        );
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@authorizationUtils.isAdmin()")
    public ResponseEntity<ApiResponse<BalanceResponse>> getUserBalance(@PathVariable Long userId) {
        BalanceResponse balance = balanceService.getUserBalance(userId);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("User balance retrieved", balance)
        );
    }
}
