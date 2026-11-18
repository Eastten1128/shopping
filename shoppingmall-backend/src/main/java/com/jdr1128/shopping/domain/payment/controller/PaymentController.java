package com.jdr1128.shopping.domain.payment.controller;

import com.jdr1128.shopping.domain.payment.dto.PaymentRequest;
import com.jdr1128.shopping.domain.payment.dto.PaymentResponse;
import com.jdr1128.shopping.domain.payment.service.PaymentFacade;
import com.jdr1128.shopping.domain.payment.service.PaymentService;
import com.jdr1128.shopping.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.apache.coyote.Request;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentFacade paymentFacade;

    public PaymentController(
            PaymentService paymentService,
            PaymentFacade paymentFacade
    ) {
        this.paymentService = paymentService;
        this.paymentFacade = paymentFacade;
    }

    //주문으로 Pay 생성 및 Ready
    /*
    @PostMapping("/api/orders/{orderId}/payments")
    public ResponseEntity<PaymentResponse> payOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request
            ){
        PaymentResponse response = paymentService.createPayment(
                userDetails.getMemberId(),
                orderId,
                request
        );

        return ResponseEntity
                .created(URI.create("/api/payments/" + response.getPayId()))
                .body(response);
    }
*/
     @PostMapping("/api/orders/{orderId}/payments")
     public ResponseEntity<PaymentResponse> payOrder(
     @AuthenticationPrincipal CustomUserDetails userDetails,
     @PathVariable Long orderId,
     @Valid @RequestBody PaymentRequest request
     ){
     PaymentResponse response = paymentFacade.createPayment(
     userDetails.getMemberId(),
     orderId,
     request
     );

     return ResponseEntity
     .created(URI.create("/api/payments/" + response.getPayId()))
     .body(response);
     }
    //Payment Ready
    @PostMapping("/api/payments/{payId}/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long payId
    ){
        PaymentResponse response = paymentService.confirmPayment(
                userDetails.getMemberId(),
                payId
        );

        return ResponseEntity
                .created(URI.create("/api/payments/confirm/" + response.getPayId())).build();
    }

    //Payment 결제 Failed
    @PostMapping("/api/payments/fail")
    public ResponseEntity<PaymentResponse> failPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long payId
    ){
        PaymentResponse response = paymentService.failPayment(
                userDetails.getMemberId(),
                payId
        );

        return ResponseEntity
                .created(URI.create("/api/payments/fail" + response.getPayId())).build();
    }

    //결제 조회
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

    //결제 취소
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
