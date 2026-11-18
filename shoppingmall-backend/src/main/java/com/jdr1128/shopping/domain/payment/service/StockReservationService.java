package com.jdr1128.shopping.domain.payment.service;

import com.jdr1128.shopping.domain.order.exception.OutOfStockException;
import com.jdr1128.shopping.domain.payment.dto.ReservationItem;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class StockReservationService {

    //redis 문자열 데이터를 저장/조회할 때 사용
    private final StringRedisTemplate redisTemplate;
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT;
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT;
    private static final DefaultRedisScript<Long> BEGIN_CONFIRM_SCRIPT;
    private static final DefaultRedisScript<Long> COMPLETE_CONFIRM_SCRIPT;

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
        RELEASE_SCRIPT = new DefaultRedisScript<>();
        BEGIN_CONFIRM_SCRIPT = new DefaultRedisScript<>();
        COMPLETE_CONFIRM_SCRIPT = new DefaultRedisScript<>();

        //Lua 스크립트 파일 경로 지정
        RESERVE_SCRIPT.setLocation(
                new ClassPathResource("redis/reserve-stock.lua")
        );
        RELEASE_SCRIPT.setLocation(
                new ClassPathResource("redis/release-stock.lua")
        );
        BEGIN_CONFIRM_SCRIPT.setLocation(
                new ClassPathResource("redis/begin-confirm-stock.lua")
        );
        COMPLETE_CONFIRM_SCRIPT.setLocation(
                new ClassPathResource("redis/complete-confirm-stock.lua")
        );

        //Lua 스크립트의 반환 타입 지정
        RESERVE_SCRIPT.setResultType(Long.class);
        RELEASE_SCRIPT.setResultType(Long.class);
        BEGIN_CONFIRM_SCRIPT.setResultType(Long.class);
        COMPLETE_CONFIRM_SCRIPT.setResultType(Long.class);
    }

    //get 재고
    public String getAvailableStock(Long productId){
        //opsForValue: String 자료형에 대한 작업을 수행할 수 있는 객체 반환
        //get/set: 값 조회/저장. increment/decrement: 숫자 1 증가/ 숫자 1 감소
        return redisTemplate.opsForValue()
                .get("inventory:available:" + productId);
    }

    // redis 결제 예약
    public boolean reserve(Long paymentId, List<ReservationItem> items){
         //결제Id가 없는 경우, 상품Id가 없는 경우, 상품 재고가 없는 경우
        if (paymentId == null || paymentId <= 0
                || items == null || items.isEmpty()
        ){
            throw new IllegalArgumentException("잘못된 예약 요청");
        }

        //같은 상품 합산, 상품을 ID순으로 정렬
        Map<Long, Integer> quantities = new TreeMap<>();

        //상품 리스트 각각 검증
        for (ReservationItem item : items){
            if(item == null
                    || item.productId() == null
                    || item.productId() <= 0
                    || item.quantity() <= 0){
                throw new IllegalArgumentException("잘못된 상품 또는 수량");
            }
            //같은 상품 합산
            try {
                quantities.merge(
                        item.productId(),
                        item.quantity(),
                        Math::addExact //범위를 벗어날경우 overflow, ArithmeticException발생
                );
            } catch(ArithmeticException e){
                throw new IllegalArgumentException("예약 수량이 너무 큽니다.", e);
            }
        }

        //중복 요청 비교에 사용할 문자열
        String canonicalItems = quantities.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));

        //Lua에 전달하기 위한 Keys 생성
        List<String> keys = new ArrayList<>();
        keys.add("reservation:" + paymentId);

        //Lua에 전달하기 위한 ARGV 생성
        List<String> args = new ArrayList<>();
        args.add(canonicalItems);

        //상품별 재고 키과 수량을 추가한다. "inventory:available:productId
        quantities.forEach((productId, quantity) -> {
            keys.add("inventory:available:" + productId);
            args.add(quantity.toString());
        });

        //Lua 실행 및 KEYS, ARGV 전달
        Long result = redisTemplate.execute(
                RESERVE_SCRIPT,
                keys,
                args.toArray()
                //오류 발견!!: toString()을 쓸 경우 구분자가 . 이므로 [101:.102:3, 2,3]"으로 전달됨.
                //toArray()로 전달하여 각 값이 별도의 인자로 전달되게 수정
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

    //결제 released
    public boolean release(Long paymentId){
         if (paymentId == null || paymentId <=0){
             throw new IllegalArgumentException("잘못된 결제 ID");
         }

         String reservationKey = "reservation:" + paymentId;

         // Lua에 전달할 재고 키를 준비하기 위한 조회
        Object storedItems = redisTemplate.opsForHash()
                .get(reservationKey, "items");

        //storedItems 타입 검사 - reservationKeys - items가 없는 경우
        if (storedItems == null){
            throw new IllegalStateException("예약 상품 내역이 없습니다.");
        }

        String itemsText = storedItems.toString();

        List<String> keys = new ArrayList<>();
        keys.add(reservationKey);
        List<String> args = new ArrayList<>();
        args.add(itemsText);

        // itemsText 상품별로 분리
        // limit: -1 -> 잘못된 끝 구분자 검증을 위해 사용
        for (String itemText : itemsText.split(",", -1)){
            String[] parts = itemText.split(":", -1);

            //형식 검사
            if (parts.length!=2){
                throw new IllegalStateException("잘못된 예약 상품 형식");
            }

            try {
                Long productId = Long.parseLong(parts[0]);
                int quantity = Integer.parseInt(parts[1]);

                if (productId <= 0 || quantity <= 0) {
                    throw new IllegalStateException("잘못된 예약 상품 또는 수량");
                }

                keys.add("inventory:available:"+productId);
                args.add(Integer.toString(quantity));
            } catch (NumberFormatException e) {
                throw new IllegalStateException("잘못된 예약 상품 숫자", e);
            }
        }

        Long result = redisTemplate.execute(
                RELEASE_SCRIPT,
                keys,
                args.toArray()
        );

        if (result == null) {
            throw new IllegalStateException("예약 해제 결과가 없습니다.");
        }

        return switch (result.intValue()){
            case 1 -> true;
            case 0 -> false;
            case -1 -> throw new IllegalStateException("예약이 없습니다.");
            case -2 -> throw new IllegalStateException("재고 초기화가 필요합니다.");
            case -3 -> throw new IllegalStateException(
                    "예약 내용이 변경되었거나 일치하지 않습니다.");
            case -4 -> throw new IllegalStateException(
                    "해제할 수 없는 예약 상태입니다.");
            default -> throw new IllegalStateException(
                    "잘못된 재고 또는 예약 데이터입니다.");
        };
    }


    //ACTIVE -> CONFIRMING
    public void beginConfirm(Long paymentId, String canonicalItems) {
        executeConfirmScript(
                BEGIN_CONFIRM_SCRIPT,
                paymentId,
                canonicalItems
        );
    }

    //CONFIRMING -> CONFIRMED
    public void completeConfirm(Long paymentId, String canonicalItems) {
        executeConfirmScript(
                COMPLETE_CONFIRM_SCRIPT,
                paymentId,
                canonicalItems
        );
    }

    //
    private void executeConfirmScript(
            DefaultRedisScript<Long> script,
            Long paymentId,
            String canonicalItems
    ) {
         if (paymentId == null || paymentId <= 0
                || canonicalItems == null || canonicalItems.isBlank()){
             throw new IllegalArgumentException("잘못된 확정 요청");
         }

         Long result = redisTemplate.execute(
                 script,
                 List.of("reservation:" + paymentId),
                 canonicalItems
         );
        if (result == null || (result != 0L && result != 1L)) {
            throw new IllegalStateException(
                    "예약 확정 처리 실패: result=" + result
            );
        }
    }
}
