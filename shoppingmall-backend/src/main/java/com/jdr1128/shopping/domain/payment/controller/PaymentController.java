package com.jdr1128.shopping.domain.payment.controller;

import com.jdr1128.shopping.domain.payment.dto.PaymentRequest;
import com.jdr1128.shopping.domain.payment.dto.PaymentResponse;
import com.jdr1128.shopping.domain.payment.service.PaymentService;
import com.jdr1128.shopping.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/orders/{orderId}/payments")
    public ResponseEntity<PaymentResponse> payOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request
            ){
        PaymentResponse response = paymentService.payOrder(
                userDetails.getMemberId(),
                orderId,
                request
        );

        return ResponseEntity
                .created(URI.create("/api/payments/" + response.getPayId()))
                .body(response);
    }

    @GetMapping("/api/payments/{payId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long payId
    ) {
        PaymentResponse response = paymentService.getPayment(
                userDetails.getMemberId(),
                payId
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/payments/{payId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long payId
    ) {
        PaymentResponse response = paymentService.cancelPayment(
                userDetails.getMemberId(),
                payId
        );

        return ResponseEntity.ok(response);
    }
}
