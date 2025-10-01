package com.example.Games.purchase;

import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.purchase.dto.PurchaseGamesRequest;
import com.example.Games.purchase.dto.PurchaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/purchase")
@RequiredArgsConstructor
@Validated
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final ResponseMapStruct responseMapper;

    @PostMapping("/game/{gameId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PurchaseResponse>> purchaseGame(@PathVariable Long gameId) {
        PurchaseResponse result = purchaseService.purchaseGame(gameId);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Game purchased successfully", result)
        );
    }

    @PostMapping("/games")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> purchaseGames(@RequestBody @Valid PurchaseGamesRequest request) {
        List<PurchaseResponse> results = purchaseService.purchaseGamesByIds(request);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse(
                        String.format("%d games purchased successfully", results.size()), 
                        results)
        );
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getMyPurchaseHistory() {
        List<PurchaseResponse> history = purchaseService.getMyPurchaseHistory();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Purchase history retrieved", history)
        );
    }

    @GetMapping("/history/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PurchaseResponse>>> getMyPurchaseHistoryPaged(Pageable pageable) {
        Page<PurchaseResponse> history = purchaseService.getMyPurchaseHistoryPaged(pageable);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Paged purchase history retrieved", history)
        );
    }

    @GetMapping("/admin/user/{userId}/history")
    @PreAuthorize("@authorizationUtils.isAdmin()")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getUserPurchaseHistory(@PathVariable Long userId) {
        List<PurchaseResponse> history = purchaseService.getUserPurchaseHistory(userId);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("User purchase history retrieved", history)
        );
    }

    @GetMapping("/admin/game/{gameId}/purchases")
    @PreAuthorize("@authorizationUtils.isAdmin()")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getGamePurchases(@PathVariable Long gameId) {
        List<PurchaseResponse> purchases = purchaseService.getGamePurchases(gameId);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Game purchase history retrieved", purchases)
        );
    }

    @GetMapping("/developer/sales")
    @PreAuthorize("@authorizationUtils.isDeveloper()")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getMySales() {
        List<PurchaseResponse> sales = purchaseService.getMySales();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Developer sales retrieved", sales)
        );
    }

    @GetMapping("/developer/revenue")
    @PreAuthorize("@authorizationUtils.isDeveloper()")
    public ResponseEntity<ApiResponse<BigDecimal>> getMyTotalRevenue() {
        BigDecimal revenue = purchaseService.getMyTotalRevenue();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Total revenue retrieved", revenue)
        );
    }
}
