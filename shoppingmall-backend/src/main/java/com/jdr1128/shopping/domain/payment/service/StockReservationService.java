package com.jdr1128.shopping.domain.payment.service;

import com.jdr1128.shopping.domain.order.exception.OutOfStockException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockReservationService {

    //redis 문자열 데이터를 저장/조회할 때 사용
    private final StringRedisTemplate redisTemplate;
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT;

    public StockReservationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    // Lua 스크립트 연결
    /*
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT =
            new DefaultRedisScript<>(
                    new ClassPathResource("redis/reserve-stock.lua"),
                    Long.class
            );
     */
    // Lua 스크립트 연결
     static {
        RESERVE_SCRIPT = new DefaultRedisScript<>();

        //Lua 스크립트 파일 경로 지정
        RESERVE_SCRIPT.setLocation(
                new ClassPathResource("redis/reserve-stock.lua")
        );

        //Lua 스크립트의 반환 타입 지정
        RESERVE_SCRIPT.setResultType(Long.class);
    }

    //get 재고
    public String getAvailableStock(Long productId){
        //opsForValue: String 자료형에 대한 작업을 수행할 수 있는 객체 반환
        //get/set: 값 조회/저장. increment/decrement: 숫자 1 증가/ 숫자 1 감소
        return redisTemplate.opsForValue()
                .get("inventory:available:" + productId);
    }

    //단일 상품 예약
    public boolean reserve(Long paymentId, Long productId, int quantity){
         //결제Id가 없는 경우, 상품Id가 없는 경우, 상품 재고가 없는 경우
        if (paymentId == null || paymentId <= 0
                || productId == null || productId <= 0
                || quantity <=0 ){
            throw new IllegalArgumentException("잘못된 예약 요청");
        }

        //Keys, ARGV 생성
        String stockKey = "inventory:available:" + productId;
        String reservationKey = "reservation:" + paymentId;

        //Lua 실행 및 KEYS, ARGV 전달
        Long result = redisTemplate.execute(
                RESERVE_SCRIPT,
                List.of(stockKey, reservationKey),
                productId.toString(),
                Integer.toString(quantity)
        );

        if( result == null){
            throw new IllegalStateException("예약 처리 결과가 없습니다.");
        }

        return switch (result.intValue()) {
            case 1 -> true;   // 신규 예약
            case 0 -> false;  // 기존 예약, 성공으로 취급하되 차감하지 않음
            case -1 -> throw new OutOfStockException();
            case -2 -> throw new IllegalStateException("재고 초기화가 필요합니다.");
            case -3 -> throw new IllegalStateException(
                    "동일 결제의 예약 내용이 다릅니다.");
            case -4 -> throw new IllegalStateException(
                    "이미 종료된 예약입니다.");
            default -> throw new IllegalStateException(
                    "잘못된 재고 또는 예약 데이터입니다.");
        };
    }
}
