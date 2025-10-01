package com.example.Games.cart;

import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.test.WebMvcTestWithoutSecurity;
import com.example.Games.cart.dto.AddToCartRequest;
import com.example.Games.cart.dto.CartOperationResponse;
import com.example.Games.cart.dto.CartSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTestWithoutSecurity(CartController.class)
@DisplayName("CartController Tests")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private ResponseMapStruct responseMapper;

    @Test
    @DisplayName("Should add game to cart successfully")
    void shouldAddGameToCartSuccessfully() throws Exception {
        // Given
        AddToCartRequest request = new AddToCartRequest(1L);
        CartOperationResponse operationResponse = CartOperationResponse.addedToCart(1L, "Test Game", 1);
        ApiResponse<CartOperationResponse> apiResponse = ApiResponse.success("Game added to cart successfully", operationResponse);

        when(cartService.addToCart(request)).thenReturn(operationResponse);
        when(responseMapper.toSuccessResponse("Game added to cart successfully", operationResponse))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Game added to cart successfully"))
                .andExpect(jsonPath("$.data.operation").value("ADD"))
                .andExpect(jsonPath("$.data.gameId").value(1))
                .andExpect(jsonPath("$.data.gameTitle").value("Test Game"))
                .andExpect(jsonPath("$.data.cartSize").value(1));

        verify(cartService).addToCart(request);
        verify(responseMapper).toSuccessResponse("Game added to cart successfully", operationResponse);
    }

    @Test
    @DisplayName("Should view cart successfully")
    void shouldViewCartSuccessfully() throws Exception {
        // Given
        CartSummaryResponse cartSummary = new CartSummaryResponse(
                List.of(), 2, new BigDecimal("59.98"), "$59.98"
        );
        ApiResponse<CartSummaryResponse> apiResponse = ApiResponse.success(cartSummary);

        when(cartService.viewCart()).thenReturn(cartSummary);
        when(responseMapper.toSuccessResponse(cartSummary)).thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/cart")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.data.totalItems").value(2))
                        .andExpect(jsonPath("$.data.totalPrice").value(59.98))
                        .andExpect(jsonPath("$.data.formattedTotalPrice").value("$59.98"));

        verify(cartService).viewCart();
        verify(responseMapper).toSuccessResponse(cartSummary);
    }

    @Test
    @DisplayName("Should remove game from cart successfully")
    void shouldRemoveGameFromCartSuccessfully() throws Exception {
        // Given
        Long gameId = 1L;
        CartOperationResponse operationResponse = CartOperationResponse.removedFromCart(1L, "Test Game", 0);
        ApiResponse<CartOperationResponse> apiResponse = ApiResponse.success("Game removed from cart successfully", operationResponse);

        when(cartService.removeFromCart(gameId)).thenReturn(operationResponse);
        when(responseMapper.toSuccessResponse("Game removed from cart successfully", operationResponse))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(delete("/api/v1/cart/remove/{gameId}", gameId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Game removed from cart successfully"))
                .andExpect(jsonPath("$.data.operation").value("REMOVE"))
                .andExpect(jsonPath("$.data.gameId").value(1))
                .andExpect(jsonPath("$.data.cartSize").value(0));

        verify(cartService).removeFromCart(gameId);
        verify(responseMapper).toSuccessResponse("Game removed from cart successfully", operationResponse);
    }

    @Test
    @DisplayName("Should checkout cart successfully")
    void shouldCheckoutCartSuccessfully() throws Exception {
        // Given
        CartOperationResponse operationResponse = CartOperationResponse.checkedOut(3, new BigDecimal("89.97"));
        ApiResponse<CartOperationResponse> apiResponse = ApiResponse.success("Checkout completed successfully", operationResponse);

        when(cartService.checkout()).thenReturn(operationResponse);
        when(responseMapper.toSuccessResponse("Checkout completed successfully", operationResponse))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/cart/checkout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Checkout completed successfully"))
                .andExpect(jsonPath("$.data.operation").value("CHECKOUT"))
                .andExpect(jsonPath("$.data.itemsProcessed").value(3))
                .andExpect(jsonPath("$.data.totalAmount").value(89.97));

        verify(cartService).checkout();
        verify(responseMapper).toSuccessResponse("Checkout completed successfully", operationResponse);
    }

    @Test
    @DisplayName("Should clear cart successfully")
    void shouldClearCartSuccessfully() throws Exception {
        // Given
        CartOperationResponse operationResponse = CartOperationResponse.cartCleared(2);
        ApiResponse<CartOperationResponse> apiResponse = ApiResponse.success("Cart cleared successfully", operationResponse);

        when(cartService.clearCart()).thenReturn(operationResponse);
        when(responseMapper.toSuccessResponse("Cart cleared successfully", operationResponse))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(delete("/api/v1/cart/clear")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart cleared successfully"))
                .andExpect(jsonPath("$.data.operation").value("CLEAR"))
                .andExpect(jsonPath("$.data.itemsProcessed").value(2));

        verify(cartService).clearCart();
        verify(responseMapper).toSuccessResponse("Cart cleared successfully", operationResponse);
    }

    @Test
    @DisplayName("Should validate cart successfully")
    void ShouldCheckoutCartSuccessfully() throws Exception {
        // Given
        boolean canCheckout = true;
        ApiResponse<Boolean> apiResponse = ApiResponse.success("Cart validation completed", canCheckout);

        when(cartService.validateCartForCheckout()).thenReturn(canCheckout);
        when(responseMapper.toSuccessResponse("Cart validation completed", canCheckout))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/cart/can-checkout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart validation completed"))
                .andExpect(jsonPath("$.data").value(true));

        verify(cartService).validateCartForCheckout();
        verify(responseMapper).toSuccessResponse("Cart validation completed", canCheckout);
    }

    @Test
    @DisplayName("Should handle invalid request parameters")
    void shouldHandleInvalidRequestParameters() throws Exception {
        // Test 1: Invalid game ID in add request
        AddToCartRequest invalidRequest = new AddToCartRequest(null);

        mockMvc.perform(post("/api/v1/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // Test 2: Invalid path parameter
        mockMvc.perform(delete("/api/v1/cart/remove/invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addToCart(any());
        verify(cartService, never()).removeFromCart(any());
    }

    @Test
    @DisplayName("Should handle service layer exceptions appropriately")
    void shouldHandleServiceLayerExceptionsAppropriately() throws Exception {
        // Test 1: Game not found
        AddToCartRequest request = new AddToCartRequest(999L);
        when(cartService.addToCart(request))
                .thenThrow(new RuntimeException("Game not found"));

        mockMvc.perform(post("/api/v1/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        // Test 2: Empty cart checkout
        when(cartService.checkout())
                .thenThrow(new RuntimeException("Cart is empty"));

        mockMvc.perform(post("/api/v1/cart/checkout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        verify(cartService).addToCart(request);
        verify(cartService).checkout();
    }
}