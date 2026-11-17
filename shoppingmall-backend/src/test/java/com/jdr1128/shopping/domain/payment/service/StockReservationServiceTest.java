package com.jdr1128.shopping.domain.payment.service;

import com.jdr1128.shopping.domain.order.exception.OutOfStockException;
import com.jdr1128.shopping.domain.payment.dto.ReservationItem;
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
    private Long productA;
    private Long productB;

    private String stockKeyA;
    private String stockKeyB;
    private String reservationKey;

    @BeforeEach
    void setUp() {
        // 테스트마다 다른 양수 ID 사용
        long base = ThreadLocalRandom.current()
                .nextLong(1, Long.MAX_VALUE - 2);

        paymentId = base;
        productA = base + 1;
        productB = base + 2;

        stockKeyA = "inventory:available:" + productA;
        stockKeyB = "inventory:available:" + productB;
        reservationKey = "reservation:" + paymentId;

        redisTemplate.opsForValue().set(stockKeyA, "10");
        redisTemplate.opsForValue().set(stockKeyB, "5");
    }

    @AfterEach
    void tearDown() {
        // 이 테스트에서 사용한 키만 삭제
        redisTemplate.delete(
                List.of(stockKeyA, stockKeyB, reservationKey)
        );
    }

    @Test
    @DisplayName("모든 상품의 재고가 충분하면 전체 예약한다")
    void reserve_success() {
        // when
        boolean created = reservationService.reserve(
                paymentId,
                List.of(
                        new ReservationItem(productA, 2),
                        new ReservationItem(productB, 3)
                )
        );

        // then
        assertThat(created).isTrue();
        assertStock("8", "2");

        assertThat(redisTemplate.opsForHash()
                .get(reservationKey, "items"))
                .isEqualTo(productA + ":2," + productB + ":3");

        assertThat(redisTemplate.opsForHash()
                .get(reservationKey, "status"))
                .isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("뒤쪽 상품이 부족해도 앞쪽 상품을 차감하지 않는다")
    void reserve_insufficientStock_changesNothing() {
        // B는 5개인데 6개 요청
        assertThatThrownBy(() ->
                reservationService.reserve(
                        paymentId,
                        List.of(
                                new ReservationItem(productA, 2),
                                new ReservationItem(productB, 6)
                        )
                )
        ).isInstanceOf(OutOfStockException.class);

        // A까지 원래 수량을 유지해야 함
        assertStock("10", "5");
        assertThat(redisTemplate.hasKey(reservationKey)).isFalse();
    }

    @Test
    @DisplayName("상품 순서가 달라도 동일 예약이면 추가 차감하지 않는다")
    void reserve_reorderedItems_isDuplicate() {
        // given
        reservationService.reserve(
                paymentId,
                List.of(
                        new ReservationItem(productA, 2),
                        new ReservationItem(productB, 3)
                )
        );

        // when: 순서를 반대로 전달
        boolean created = reservationService.reserve(
                paymentId,
                List.of(
                        new ReservationItem(productB, 3),
                        new ReservationItem(productA, 2)
                )
        );

        // then
        assertThat(created).isFalse();
        assertStock("8", "2");
    }

    @Test
    @DisplayName("중복 상품은 합산하고 합산된 재요청도 동일하게 인식한다")
    void reserve_duplicateProducts_areMerged() {
        // when: A 1개 + A 2개 = A 3개
        boolean first = reservationService.reserve(
                paymentId,
                List.of(
                        new ReservationItem(productA, 1),
                        new ReservationItem(productB, 2),
                        new ReservationItem(productA, 2)
                )
        );

        boolean retry = reservationService.reserve(
                paymentId,
                List.of(
                        new ReservationItem(productA, 3),
                        new ReservationItem(productB, 2)
                )
        );

        // then
        assertThat(first).isTrue();
        assertThat(retry).isFalse();
        assertStock("7", "3");

        assertThat(redisTemplate.opsForHash()
                .get(reservationKey, "items"))
                .isEqualTo(productA + ":3," + productB + ":2");
    }

    @Test
    @DisplayName("같은 결제의 수량이 변경되면 기존 예약을 유지하고 거절한다")
    void reserve_changedQuantity_rejected() {
        // given
        reservationService.reserve(
                paymentId,
                List.of(
                        new ReservationItem(productA, 2),
                        new ReservationItem(productB, 3)
                )
        );

        // when / then
        assertThatThrownBy(() ->
                reservationService.reserve(
                        paymentId,
                        List.of(
                                new ReservationItem(productA, 3),
                                new ReservationItem(productB, 3)
                        )
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("동일 결제의 예약 내용이 다릅니다.");

        assertStock("8", "2");

        assertThat(redisTemplate.opsForHash()
                .get(reservationKey, "items"))
                .isEqualTo(productA + ":2," + productB + ":3");

        assertThat(redisTemplate.opsForHash()
                .get(reservationKey, "status"))
                .isEqualTo("ACTIVE");
    }

    private void assertStock(String expectedA, String expectedB) {
        assertThat(redisTemplate.opsForValue().get(stockKeyA))
                .isEqualTo(expectedA);

        assertThat(redisTemplate.opsForValue().get(stockKeyB))
                .isEqualTo(expectedB);
    }
}