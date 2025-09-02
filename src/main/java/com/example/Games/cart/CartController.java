package com.example.Games.cart;

import com.example.Games.cart.dto.AddToCartRequest;
import com.example.Games.cart.dto.CartItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<Void> addToCart(@RequestBody @Valid AddToCartRequest request) {
        cartService.addToCart(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<CartItemResponse>> viewCart() {
        return ResponseEntity.ok(cartService.viewCart());
    }

    @DeleteMapping("/remove/{gameId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long gameId) {
        cartService.removeFromCart(gameId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<String> checkout() {
        cartService.checkout();
        return ResponseEntity.ok("Checkout successful. Games added to library.");
    }
}
