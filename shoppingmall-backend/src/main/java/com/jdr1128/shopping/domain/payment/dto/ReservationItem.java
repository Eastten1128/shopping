package com.jdr1128.shopping.domain.payment.dto;

import java.util.Map;
import java.util.TreeMap;

//Redis 상품 객체 예약 dto
//record는 생성자와 내부 인자 접근 메서드를 자동으로 제공하는 데이터 전달 객체
public record ReservationItem(
        Long productId, //상품Id
        int quantity    //수량
) {
}
