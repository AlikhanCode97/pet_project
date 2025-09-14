package com.example.Games.cart;

import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.cart.dto.AddToCartRequest;
import com.example.Games.cart.dto.CartOperationResponse;
import com.example.Games.cart.dto.CartSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final ResponseMapStruct responseMapper;

    @PostMapping("/add")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartOperationResponse>> addToCart(@RequestBody @Valid AddToCartRequest request) {
        CartOperationResponse result = cartService.addToCart(request);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Game added to cart successfully", result)
        );
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartSummaryResponse>> viewCart() {
        CartSummaryResponse cart = cartService.viewCart();
        return ResponseEntity.ok(responseMapper.toSuccessResponse(cart));
    }

    @DeleteMapping("/remove/{gameId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartOperationResponse>> removeFromCart(@PathVariable Long gameId) {
        CartOperationResponse result = cartService.removeFromCart(gameId);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Game removed from cart successfully", result)
        );
    }

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartOperationResponse>> checkout() {
        CartOperationResponse result = cartService.checkout();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Checkout completed successfully", result)
        );
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount() {
        int count = cartService.getCartItemCount();
        return ResponseEntity.ok(responseMapper.toSuccessResponse(count));
    }

    @DeleteMapping("/clear")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartOperationResponse>> clearCart() {
        CartOperationResponse result = cartService.clearCart();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Cart cleared successfully", result)
        );
    }

    @PostMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> validateCart() {
        boolean canCheckout = cartService.validateCartForCheckout();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Cart validation completed", canCheckout)
        );
    }

    @GetMapping("/validation-details")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartService.CartValidationResponse>> getCartValidationDetails() {
        CartService.CartValidationResponse details = cartService.getCartValidationDetails();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Cart validation details retrieved", details)
        );
    }
}
