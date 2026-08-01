package com.jdr1128.shopping.domain.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class OrderCreateRequest {

    @NotBlank(message = "수취인 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    private String phoneNumber;

    @NotBlank(message = "주소는 필수입니다.")
    private String address1;

    private String address2;

    private String requestedTerm;

    public OrderCreateRequest() {}
}
