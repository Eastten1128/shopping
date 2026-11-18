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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
    @DisplayName("동일 예약의 동시 해제 요청은 한 번만 재고를 반환한다")
    void release_concurrent_onlyOnce() throws Exception {
        // given
        givenActiveReservation();
        assertReservationStatus("ACTIVE");

        //20회 결제 예약 요청
        int requestCount = 20;

        //스레드 생성
        ExecutorService executor =
                Executors.newFixedThreadPool(requestCount);

        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            // 작업 등록
            for (int i = 0; i < requestCount; i++) {
                Future<Boolean> future = executor.submit(() -> {
                    // 이 작업은 출발 준비 완료
                    ready.countDown();

                    // 출발 신호까지 대기
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("출발 신호 대기 시간 초과");
                    }

                    return reservationService.release(paymentId);
                });

                futures.add(future);
            }

            // 모든 작업이 출발 준비를 마칠 때까지 대기
            assertThat(ready.await(10, TimeUnit.SECONDS))
                    .as("모든 작업이 출발 준비를 마쳐야 한다")
                    .isTrue();

            // when: 대기 중인 작업들을 함께 출발시킴
            start.countDown();

            // 추가 작업 접수 종료, 기존 작업은 계속 실행
            executor.shutdown();

            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS))
                    .as("모든 해제 작업이 제한 시간 안에 종료되어야 한다")
                    .isTrue();

            // then: 모든 작업 결과 확인
            int releasedCount = 0;
            int duplicateCount = 0;

            for (Future<Boolean> future : futures) {
                boolean released = future.get();

                if (released) {
                    releasedCount++;
                } else {
                    duplicateCount++;
                }
            }

            assertThat(releasedCount).isEqualTo(1);
            assertThat(duplicateCount).isEqualTo(requestCount - 1);

            // 재고는 한 번만 복구되어야 함
            assertStock("10", "5");
            assertReservationStatus("RELEASED");

            // 예약 내용은 유지되어야 함
            assertThat(redisTemplate.opsForHash()
                    .get(reservationKey, "items"))
                    .isEqualTo(productA + ":2," + productB + ":3");

        } finally {
            // 중간 실패 시에도 대기 작업과 스레드 정리
            start.countDown();
            executor.shutdownNow();

            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
                    .as("테스트 종료 전에 작업 스레드가 정리되어야 한다")
                    .isTrue();
        }
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

    // Release() 테스트

    @Test
    @DisplayName("예약 해제 시 모든 재고를 반환하고 RELEASED로 변경한다")
    void release_success() {
        // given
        givenActiveReservation();

        // when
        boolean released = reservationService.release(paymentId);

        // then
        assertThat(released).isTrue();
        assertStock("10", "5");
        assertReservationStatus("RELEASED");

        // 중복 요청을 구분하기 위해 예약 내용은 유지
        assertThat(redisTemplate.opsForHash()
                .get(reservationKey, "items"))
                .isEqualTo(productA + ":2," + productB + ":3");
    }

    @Test
    @DisplayName("동일 예약을 다시 해제해도 재고를 추가 반환하지 않는다")
    void release_duplicate() {
        // given
        givenActiveReservation();

        // when
        boolean first = reservationService.release(paymentId);
        boolean second = reservationService.release(paymentId);

        // then
        assertThat(first).isTrue();
        assertThat(second).isFalse();

        assertStock("10", "5");
        assertReservationStatus("RELEASED");
    }

    @Test
    @DisplayName("예약이 없으면 해제를 거절한다")
    void release_missingReservation() {
        // given
        assertThat(redisTemplate.hasKey(reservationKey)).isFalse();

        // when / then
        assertThatThrownBy(() ->
                reservationService.release(paymentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("예약 상품 내역이 없습니다.");

        assertStock("10", "5");
        assertThat(redisTemplate.hasKey(reservationKey)).isFalse();
    }

    @Test
    @DisplayName("확정 중이거나 확정된 예약은 반환하지 않는다")
    void release_confirmingOrConfirmed_rejected() {
        // given
        givenActiveReservation();

        for (String status : List.of("CONFIRMING", "CONFIRMED")) {
            redisTemplate.opsForHash()
                    .put(reservationKey, "status", status);

            // when / then
            assertThatThrownBy(() ->
                    reservationService.release(paymentId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("해제할 수 없는 예약 상태입니다.");

            assertStock("8", "2");
            assertReservationStatus(status);
        }
    }

    @Test
    @DisplayName("일부 재고 키가 없으면 다른 상품도 반환하지 않는다")
    void release_missingStock_changesNothing() {
        // given
        givenActiveReservation();
        redisTemplate.delete(stockKeyB);

        // when / then
        assertThatThrownBy(() ->
                reservationService.release(paymentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("재고 초기화가 필요합니다.");

        // A를 먼저 반환하지 않았는지 확인
        assertThat(redisTemplate.opsForValue().get(stockKeyA))
                .isEqualTo("8");

        // 누락된 키를 임의로 생성하지 않아야 함
        assertThat(redisTemplate.hasKey(stockKeyB)).isFalse();

        // 반환이 완료되지 않았으므로 ACTIVE 유지
        assertReservationStatus("ACTIVE");
    }

    @Test
    @DisplayName("일부 재고 값이 잘못되면 전체 반환을 거절한다")
    void release_invalidStock_changesNothing() {
        // given
        givenActiveReservation();
        redisTemplate.opsForValue().set(stockKeyB, "invalid");

        // when / then
        assertThatThrownBy(() ->
                reservationService.release(paymentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잘못된 재고 또는 예약 데이터입니다.");

        assertThat(redisTemplate.opsForValue().get(stockKeyA))
                .isEqualTo("8");

        assertThat(redisTemplate.opsForValue().get(stockKeyB))
                .isEqualTo("invalid");

        assertReservationStatus("ACTIVE");
    }

    // Confirm 테스트: 실제 Redis/Lua 상태 전환 검증 (MySQL 확정 테스트는 별도)
    @Test
    @DisplayName("확정 시작 시 ACTIVE에서 CONFIRMING으로 변경한다")
    void confirm_beginChangesStatus() {
        // given
        givenActiveReservation();

        // when
        reservationService.beginConfirm(paymentId, canonicalItems());

        // then
        assertReservationStatus("CONFIRMING");
        assertStock("8", "2");
        assertThat(redisTemplate.opsForHash().get(reservationKey, "items"))
                .isEqualTo(canonicalItems());
    }

    @Test
    @DisplayName("확정 완료 시 CONFIRMING에서 CONFIRMED로 변경한다")
    void confirm_completeChangesStatus() {
        // given
        givenActiveReservation();
        reservationService.beginConfirm(paymentId, canonicalItems());

        // when
        reservationService.completeConfirm(paymentId, canonicalItems());

        // then
        assertReservationStatus("CONFIRMED");
        assertStock("8", "2");
        assertThat(redisTemplate.opsForHash().get(reservationKey, "items"))
                .isEqualTo(canonicalItems());
    }

    @Test
    @DisplayName("확정 시작과 완료를 재요청해도 재고를 추가 차감하지 않는다")
    void confirm_repeatedCallsAreSafe() {
        // given
        givenActiveReservation();

        // when
        reservationService.beginConfirm(paymentId, canonicalItems());
        reservationService.beginConfirm(paymentId, canonicalItems());

        assertReservationStatus("CONFIRMING");
        assertStock("8", "2");

        reservationService.completeConfirm(paymentId, canonicalItems());
        reservationService.completeConfirm(paymentId, canonicalItems());

        // then
        assertReservationStatus("CONFIRMED");
        assertStock("8", "2");
        assertThat(redisTemplate.opsForHash().get(reservationKey, "items"))
                .isEqualTo(canonicalItems());
    }

    @Test
    @DisplayName("ACTIVE에서 CONFIRMED로 바로 변경할 수 없다")
    void confirm_cannotSkipConfirming() {
        // given
        givenActiveReservation();

        // when / then
        assertThatThrownBy(() ->
                reservationService.completeConfirm(paymentId, canonicalItems()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("예약 확정 처리 실패: result=-4");

        assertReservationStatus("ACTIVE");
        assertStock("8", "2");
        assertThat(redisTemplate.opsForHash().get(reservationKey, "items"))
                .isEqualTo(canonicalItems());
    }

    @Test
    @DisplayName("예약 내용이 다르면 확정 시작을 거절한다")
    void confirm_differentItemsRejected() {
        // given
        givenActiveReservation();
        String wrongItems = productA + ":3," + productB + ":3";

        // when / then
        assertThatThrownBy(() ->
                reservationService.beginConfirm(paymentId, wrongItems))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("예약 확정 처리 실패: result=-3");

        assertReservationStatus("ACTIVE");
        assertStock("8", "2");
        assertThat(redisTemplate.opsForHash().get(reservationKey, "items"))
                .isEqualTo(canonicalItems());
    }

    @Test
    @DisplayName("CONFIRMING 상태의 예약은 해제하지 않는다")
    void confirm_releaseIsBlocked() {
        // given
        givenActiveReservation();
        reservationService.beginConfirm(paymentId, canonicalItems());

        // when / then
        assertThatThrownBy(() -> reservationService.release(paymentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("해제할 수 없는 예약 상태입니다.");

        assertReservationStatus("CONFIRMING");
        assertStock("8", "2");
        assertThat(redisTemplate.opsForHash().get(reservationKey, "items"))
                .isEqualTo(canonicalItems());
    }

    private String canonicalItems() {
        return productA + ":2," + productB + ":3";
    }

    //given
    private void givenActiveReservation() {
        reservationService.reserve(
                paymentId,
                List.of(
                        new ReservationItem(productA, 2),
                        new ReservationItem(productB, 3)
                )
        );

        assertStock("8", "2");
    }
    private void assertReservationStatus(String expected) {
        assertThat(redisTemplate.opsForHash()
                .get(reservationKey, "status"))
                .isEqualTo(expected);
    }

} //End of Class