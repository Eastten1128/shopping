package com.jdr1128.shopping.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PaymentRequest {

    @NotBlank(message = "카드사는 필수입니다.")
    private String cardCompany;

    @NotBlank(message = "카드번호는 필수입니다.")
    private String cardNumber;

    public PaymentRequest(){}

}
