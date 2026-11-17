package com.jdr1128.shopping.domain.payment.service;

import com.jdr1128.shopping.domain.order.exception.OutOfStockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class StockReservationServiceTest {

    @Autowired
    private StockReservationService reservationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Long paymentId;
    private Long productId;
    private String stockKey;
    private String reservationKey;

    @BeforeEach
    void setUp() {
        //결제Id, 상품Id
        paymentId = randomId();
        productId = randomId();

        //재고 키, 예약 키 생성
        stockKey = "inventory:available:" + productId;
        reservationKey = "reservation:" + paymentId;

        //기본 재고 수량 10 설정
        redisTemplate.opsForValue().set(stockKey, "10");
    }

    @AfterEach
    void tearDown() {
        //테스트 완료 시 redis 데이터 삭제
        redisTemplate.delete(List.of(stockKey, reservationKey));
    }

    @Test
    @DisplayName("최초 예약은 재고를 차감하고 ACTIVE 예약을 저장한다")
    void reserve_success() {
        // when
        //redis에 결제 요청(결제, 상품, 수량) 주입
        boolean created =
                reservationService.reserve(paymentId, productId, 2);

        // then
        //created가 성공했는지 검사
        assertThat(created).isTrue();
        //redis에서 재고가 감소했는지 검사
        assertThat(redisTemplate.opsForValue().get(stockKey))
                .isEqualTo("8");
        //결제Id의 상품Id 검사
        assertThat(redisTemplate.opsForHash()
                .get(reservationKey, "productId"))
                .isEqualTo(productId.toString());
        //결제Id의 수량 검사
        assertThat(redisTemplate.opsForHash()
                .get(reservationKey, "quantity"))
                .isEqualTo("2");
        //결제Id의 상태 검사(ACTIVE)
        assertThat(redisTemplate.opsForHash()
                .get(reservationKey, "status"))
                .isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("같은 결제의 동일 요청은 재고를 다시 차감하지 않는다")
    void reserve_duplicate() {
        // given
        //1번째 결제 요청
        reservationService.reserve(paymentId, productId, 2);

        // when
        // 2번째 결제 요청
        boolean created =
                reservationService.reserve(paymentId, productId, 2);

        // then
        //결제가 실패됐는지
        assertThat(created).isFalse();
        //남은 재고가 8개인지 확인
        assertThat(redisTemplate.opsForValue().get(stockKey))
                .isEqualTo("8");
        // 결제 요청의 재고가 2개인지 확인
        assertThat(redisTemplate.opsForHash()
                .get(reservationKey, "quantity"))
                .isEqualTo("2");
    }

    @Test
    @DisplayName("같은 결제에서 수량이 달라지면 거절한다")
    void reserve_differentQuantity() {
        // given
        reservationService.reserve(paymentId, productId, 2);

        // when / then
        assertThatThrownBy(() ->
                reservationService.reserve(paymentId, productId, 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("동일 결제의 예약 내용이 다릅니다.");

        //요청이 올바르게 입력돼서 남은 재고가 8개인지 확인
        assertThat(redisTemplate.opsForValue().get(stockKey))
                .isEqualTo("8");

        //결제 요청이 올바르게 입력돼서 주문량이 2개인지 확인
        assertThat(redisTemplate.opsForHash()
                .get(reservationKey, "quantity"))
                .isEqualTo("2");
    }

    @Test
    @DisplayName("재고가 부족하면 차감하거나 예약을 생성하지 않는다")
    void reserve_outOfStock() {
        // when / then
        //11개 주문
        assertThatThrownBy(() ->
                reservationService.reserve(paymentId, productId, 11))
                .isInstanceOf(OutOfStockException.class);

        //올바르지 않은 주문 - 재고가 그대로인지 확인
        assertThat(redisTemplate.opsForValue().get(stockKey))
                .isEqualTo("10");
        //결제요청이 거부되었는지 확인
        assertThat(redisTemplate.hasKey(reservationKey)).isFalse();
    }

    @Test
    @DisplayName("재고 키가 없으면 초기화 오류를 반환한다")
    void reserve_missingStock() {
        // given
        redisTemplate.delete(stockKey);

        // when / then
        assertThatThrownBy(() ->
                reservationService.reserve(paymentId, productId, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("재고 초기화가 필요합니다.");

        assertThat(redisTemplate.hasKey(stockKey)).isFalse();
        assertThat(redisTemplate.hasKey(reservationKey)).isFalse();
    }

    //랜덤 Id 생성(1~Long.MAX_VALUE)
    private Long randomId() {
        return ThreadLocalRandom.current()
                .nextLong(1, Long.MAX_VALUE);
    }
}