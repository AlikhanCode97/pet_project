package com.example.Games.purchase;

import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.purchase.dto.PurchaseResponse;
import com.example.Games.purchase.dto.PurchaseStatsResponse;
import com.example.Games.game.dto.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final ResponseMapStruct responseMapper;

    // ============= PURCHASE OPERATIONS =============

    @PostMapping("/game/{gameId}")
    public ResponseEntity<ApiResponse<PurchaseResponse>> purchaseGame(@PathVariable Long gameId) {
        PurchaseResponse result = purchaseService.purchaseGame(gameId);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Game purchased successfully", result)
        );
    }

    @PostMapping("/games")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> purchaseGames(@RequestBody List<Long> gameIds) {
        List<PurchaseResponse> results = purchaseService.purchaseGamesByIds(gameIds);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse(
                        String.format("%d games purchased successfully", results.size()), 
                        results)
        );
    }

    // ============= PURCHASE HISTORY =============

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getMyPurchaseHistory() {
        List<PurchaseResponse> history = purchaseService.getMyPurchaseHistory();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Purchase history retrieved", history)
        );
    }

    @GetMapping("/history/paged")
    public ResponseEntity<ApiResponse<Page<PurchaseResponse>>> getMyPurchaseHistoryPaged(Pageable pageable) {
        Page<PurchaseResponse> history = purchaseService.getMyPurchaseHistoryPaged(pageable);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Paged purchase history retrieved", history)
        );
    }

    @GetMapping("/history/date-range")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getPurchasesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<PurchaseResponse> purchases = purchaseService.getPurchasesByDateRange(startDate, endDate);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Purchases retrieved for date range", purchases)
        );
    }

    // ============= STATISTICS =============

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PurchaseStatsResponse>> getMyPurchaseStats() {
        PurchaseStatsResponse stats = purchaseService.getMyPurchaseStats();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Purchase statistics retrieved", stats)
        );
    }

    // ============= DEVELOPER OPERATIONS =============

    @GetMapping("/admin/user/{userId}/history")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getUserPurchaseHistory(@PathVariable Long userId) {
        List<PurchaseResponse> history = purchaseService.getUserPurchaseHistory(userId);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("User purchase history retrieved", history)
        );
    }

    @GetMapping("/admin/game/{gameId}/purchases")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getGamePurchases(@PathVariable Long gameId) {
        List<PurchaseResponse> purchases = purchaseService.getGamePurchases(gameId);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Game purchase history retrieved", purchases)
        );
    }

    @GetMapping("/developer/sales")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getMySales() {
        List<PurchaseResponse> sales = purchaseService.getMySales();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Developer sales retrieved", sales)
        );
    }

    @GetMapping("/developer/revenue")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<ApiResponse<BigDecimal>> getMyTotalRevenue() {
        BigDecimal revenue = purchaseService.getMyTotalRevenue();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Total revenue retrieved", revenue)
        );
    }
}
