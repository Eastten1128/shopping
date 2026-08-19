package com.jdr1128.shopping.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PaymentRequest {

    @NotBlank(message = "카드사는 필수입니다.")
    private String cardCompany;

    @NotBlank(message = "카드번호는 필수입니다.")
    private String cardNumber;

    /**
     * 모의 결제 테스트용 필드
     * true 또는 값을 생략하면 결제 성공으로 처리한다
     * false를 보내면 결제 실패를 처리합니다.
     *
     * 실제 PG 연동에서는 이 필드를 클라이언트 보내는 방식이 아니라
     * 결제사 응답값으로 성공/실패를 판단한다
     */
    private Boolean paymentSuccess = true;

    public PaymentRequest(){}


    public boolean isPaymentSuccess(){
        return paymentSuccess == null || paymentSuccess;
    }
}
