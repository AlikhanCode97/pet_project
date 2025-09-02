package com.example.Games.user.balance;

import com.example.Games.user.balance.dto.DepositRequest;
import com.example.Games.user.balance.dto.WithdrawRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/balance")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping
    public ResponseEntity<BigDecimal> getBalance() {
        return ResponseEntity.ok(balanceService.getBalance());
    }

    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestBody @Valid DepositRequest request) {
        BigDecimal newBalance = balanceService.deposit(request);
        return ResponseEntity.ok("Deposit successful. New balance: " + newBalance);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> withdraw(@RequestBody @Valid WithdrawRequest request) {
        BigDecimal newBalance = balanceService.withdraw(request);
        return ResponseEntity.ok("Withdraw successful. New balance: " + newBalance);
    }
}
