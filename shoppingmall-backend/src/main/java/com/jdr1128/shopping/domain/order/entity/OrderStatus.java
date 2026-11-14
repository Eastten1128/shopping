package com.jdr1128.shopping.domain.order.entity;

public enum OrderStatus {
    ORDERED,    //주문 생성됨
    PAID,   //  주문 완료
    CANCELLED, //주문취소
    FAILED  //주문 실패
}
