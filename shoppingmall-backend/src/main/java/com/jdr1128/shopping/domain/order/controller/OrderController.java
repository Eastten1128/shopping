package com.jdr1128.shopping.domain.order.controller;

import com.jdr1128.shopping.domain.order.dto.OrderCreateRequest;
import com.jdr1128.shopping.domain.order.dto.OrderResponse;
import com.jdr1128.shopping.domain.order.service.OrderService;
import com.jdr1128.shopping.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        OrderResponse response = orderService.createOrder(
                userDetails.getMemberId(),
                request
        );

        return ResponseEntity
                .created(URI.create("/api/orders/" + response.getOrderId()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        List<OrderResponse> responses = orderService.getMyOrders(
                userDetails.getMemberId()
        );

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getMyOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId
    ) {
            OrderResponse response = orderService.getMyOrder(
                    userDetails.getMemberId(),
                    orderId
            );

            return ResponseEntity.ok(response);
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId
    ) {
        OrderResponse response = orderService.cancelOrder(
                userDetails.getMemberId(),
                orderId
        );

        return ResponseEntity.ok(response);
    }
}
