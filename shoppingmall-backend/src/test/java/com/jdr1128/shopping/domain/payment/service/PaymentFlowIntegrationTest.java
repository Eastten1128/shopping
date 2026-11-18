package com.jdr1128.shopping.domain.payment.service;

import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.category.repository.CategoryRepository;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.domain.order.entity.Order;
import com.jdr1128.shopping.domain.order.entity.OrderProduct;
import com.jdr1128.shopping.domain.order.entity.OrderStatus;
import com.jdr1128.shopping.domain.order.exception.OutOfStockException;
import com.jdr1128.shopping.domain.order.repository.OrderProductRepository;
import com.jdr1128.shopping.domain.order.repository.OrderRepository;
import com.jdr1128.shopping.domain.payment.dto.PaymentRequest;
import com.jdr1128.shopping.domain.payment.dto.PaymentResponse;
import com.jdr1128.shopping.domain.payment.entity.PaymentStatus;
import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import com.jdr1128.shopping.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

// 실제 커밋과 afterCommit을 검증하므로 클래스/테스트에 @Transactional을 붙이지 않는다.
// Redis 6380은 테스트 전용 인스턴스여야 한다. MySQL 락 호환성은 별도 검증 대상이다.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment-flow-test;DB_CLOSE_DELAY=-1",
        "spring.data.redis.host=127.0.0.1",
        "spring.data.redis.port=6380",
        "spring.data.redis.database=0",
        "spring.data.redis.timeout=2s",
        "spring.data.redis.connect-timeout=2s"
})
@ActiveProfiles("test")
public class PaymentFlowIntegrationTest {

    @Autowired private PaymentFacade paymentFacade;
    @Autowired private PaymentService paymentService;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private ProductRepository productRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderProductRepository orderProductRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    // 평소에는 실제 Lua를 실행하고, 장애 테스트에서만 최종 반영을 실패시킨다.
    @MockitoSpyBean
    private StockReservationService stockReservationService;

    private final Set<String> redisKeys = ConcurrentHashMap.newKeySet();
    private TransactionTemplate tx;
    private Fixture fixture;
    private PaymentRequest request;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        fixture = tx.execute(status -> {
            String suffix = UUID.randomUUID().toString();
            Member buyer = memberRepository.save(new Member(
                    "flow-buyer-" + suffix, "test-only-password",
                    "buyer-" + suffix + "@test.com", Role.USER));
            Member seller = memberRepository.save(new Member(
                    "flow-seller-" + suffix, "test-only-password",
                    "seller-" + suffix + "@test.com", Role.SELLER));
            Category category = categoryRepository.save(new Category("flow-" + suffix));
            Product productA = productRepository.save(new Product(
                    category, seller, "상품 A", 1000, 10, ProductStatus.SELLING));
            Product productB = productRepository.save(new Product(
                    category, seller, "상품 B", 2000, 5, ProductStatus.SELLING));
            Order order = orderRepository.save(new Order(
                    buyer, "테스트 구매자", "010-0000-0000",
                    "테스트 주소", "", "", 8000));
            orderProductRepository.saveAll(List.of(
                    new OrderProduct(order, productA, "상품 A", 2, 1000),
                    new OrderProduct(order, productB, "상품 B", 3, 2000)
            ));
            return new Fixture(buyer.getMemberId(), seller.getMemberId(),
                    category.getCategoryId(), productA.getProductId(),
                    productB.getProductId(), order.getOrderId());
        });
        assertThat(fixture).isNotNull();

        request = new PaymentRequest();
        ReflectionTestUtils.setField(request, "cardCompany", "test-card");
        ReflectionTestUtils.setField(request, "cardNumber", "****-1234");

        // 롤백되어 DB에 남지 않는 결제 ID도 추적해 해당 테스트의 키만 정리한다.
        doAnswer(invocation -> {
            Long paymentId = invocation.getArgument(0);
            redisKeys.add(reservationKey(paymentId));
            return invocation.callRealMethod();
        }).when(stockReservationService).reserve(anyLong(), anyList());

        putStock(fixture.productAId(), "10");
        putStock(fixture.productBId(), "5");
    }

    @AfterEach
    void tearDown() {
        try {
            if (!redisKeys.isEmpty()) {
                redisTemplate.delete(new ArrayList<>(redisKeys));
            }
        } finally {
            if (fixture != null) {
                // 실제 커밋을 했으므로 외래 키 순서대로 자신이 만든 행만 삭제한다.
                tx.executeWithoutResult(status -> {
                    jdbc.update("delete from payments where order_id = ?", fixture.orderId());
                    jdbc.update("delete from order_products where order_id = ?", fixture.orderId());
                    jdbc.update("delete from orders where order_id = ?", fixture.orderId());
                    jdbc.update("delete from products where product_id in (?, ?)",
                            fixture.productAId(), fixture.productBId());
                    jdbc.update("delete from categories where category_id = ?", fixture.categoryId());
                    jdbc.update("delete from members where member_id in (?, ?)",
                            fixture.buyerId(), fixture.sellerId());
                });
            }
        }
    }

    @Test
    @DisplayName("결제 생성 후 확정하면 실제 커밋과 Redis 확정이 반영된다")
    void createAndConfirm_success() {
        PaymentResponse created = createReservedPayment();

        paymentService.confirmPayment(fixture.buyerId(), created.getPayId());

        assertCompleted(created.getPayId());
    }

    @Test
    @DisplayName("동일 결제를 재확정해도 DB와 Redis 재고를 다시 차감하지 않는다")
    void confirm_twiceDoesNotDeductAgain() {
        PaymentResponse created = createReservedPayment();
        paymentService.confirmPayment(fixture.buyerId(), created.getPayId());

        PaymentResponse repeated =
                paymentService.confirmPayment(fixture.buyerId(), created.getPayId());

        assertThat(repeated.getPayStatus()).isEqualTo(PaymentStatus.COMPLETE);
        assertCompleted(created.getPayId());
    }

    @Test
    @DisplayName("Redis 재고 부족 시 결제 저장을 롤백하고 부분 예약하지 않는다")
    void create_outOfStockRollsBackPayment() {
        putStock(fixture.productBId(), "1");

        assertThatThrownBy(() ->
                paymentFacade.createPayment(fixture.buyerId(), fixture.orderId(), request))
                .isInstanceOf(OutOfStockException.class);

        assertNoPayment();
        assertDbStocks(10, 5);
        assertRedisStocks("10", "1");
        assertOrderStatus(OrderStatus.ORDERED);
        assertThat(redisKeys.stream().filter(key -> key.startsWith("reservation:")))
                .hasSize(1)
                .allSatisfy(key -> assertThat(redisTemplate.hasKey(key)).isFalse());
    }

    @Test
    @DisplayName("예약 성공 후 DB 트랜잭션이 롤백되면 보상 해제가 실행된다")
    void create_outerRollbackReleasesReservation() {
        Long paymentId = tx.execute(status -> {
            PaymentResponse created = paymentFacade.createPayment(
                    fixture.buyerId(), fixture.orderId(), request);
            assertRedisStocks("8", "2");
            status.setRollbackOnly();
            return created.getPayId();
        });

        assertThat(paymentId).isNotNull();
        assertNoPayment();
        assertDbStocks(10, 5);
        assertRedisStocks("10", "5");
        assertOrderStatus(OrderStatus.ORDERED);
        assertReservation(paymentId, "RELEASED");
    }

    @Test
    @DisplayName("두 번째 상품의 DB 재고 부족 시 첫 번째 상품 차감도 롤백된다")
    void confirm_insufficientDbStockRollsBackAllChanges() {
        PaymentResponse created = createReservedPayment();
        // Redis와 DB 불일치를 테스트에서만 주입한다. 변경값 1을 먼저 커밋한다.
        tx.executeWithoutResult(status -> jdbc.update(
                "update products set stock = ? where product_id = ?",
                1, fixture.productBId()));

        assertThatThrownBy(() ->
                paymentService.confirmPayment(fixture.buyerId(), created.getPayId()))
                .isInstanceOf(OutOfStockException.class);

        assertDbStocks(10, 1);
        assertPaymentStatus(created.getPayId(), PaymentStatus.READY);
        assertOrderStatus(OrderStatus.ORDERED);
        assertRedisStocks("8", "2");
        assertReservation(created.getPayId(), "CONFIRMING");
    }

    @Test
    @DisplayName("DB 커밋 후 Redis 확정 실패는 재차 차감 없이 재시도로 복구한다")
    void confirm_redisFailureCanBeRetriedWithoutDbDeduction() {
        PaymentResponse created = createReservedPayment();
        Long paymentId = created.getPayId();
        // 다른 서비스/Repository/Lua는 실제 실행. 첫 Redis 최종 반영만 실패시킨다.
        doThrow(new IllegalStateException("테스트용 Redis 최종 반영 실패"))
                .doCallRealMethod()
                .when(stockReservationService)
                .completeConfirm(eq(paymentId), anyString());

        paymentService.confirmPayment(fixture.buyerId(), paymentId);

        assertDbStocks(8, 2);
        assertPaymentStatus(paymentId, PaymentStatus.COMPLETE);
        assertOrderStatus(OrderStatus.PAID);
        assertRedisStocks("8", "2");
        assertReservation(paymentId, "CONFIRMING");

        // 현재 구현의 요청 재시도 복구를 검증한다. 자동 재처리 기능을 의미하지 않는다.
        paymentService.confirmPayment(fixture.buyerId(), paymentId);

        assertCompleted(paymentId);
    }

    @Test
    @DisplayName("동일 결제의 동시 확정 요청은 DB 실재고를 한 번만 차감한다")
    void confirm_concurrentRequestsDeductOnce() throws Exception {
        PaymentResponse created = createReservedPayment();
        Long paymentId = created.getPayId();
        int requestCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<PaymentResponse>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("확정 시작 신호 시간 초과");
                    }
                    return paymentService.confirmPayment(fixture.buyerId(), paymentId);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

            // 작업 스레드의 예외도 반드시 회수하여 테스트 실패로 반영한다.
            for (Future<PaymentResponse> future : futures) {
                PaymentResponse response = future.get();
                assertThat(response.getPayId()).isEqualTo(paymentId);
                assertThat(response.getPayStatus()).isEqualTo(PaymentStatus.COMPLETE);
                assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PAID);
            }
            assertCompleted(paymentId);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private PaymentResponse createReservedPayment() {
        PaymentResponse created = paymentFacade.createPayment(
                fixture.buyerId(), fixture.orderId(), request);
        assertThat(created.getPayPrice()).isEqualTo(8000);
        assertPaymentStatus(created.getPayId(), PaymentStatus.READY);
        assertOrderStatus(OrderStatus.ORDERED);
        assertDbStocks(10, 5);
        assertRedisStocks("8", "2");
        assertReservation(created.getPayId(), "ACTIVE");
        return created;
    }

    private void assertCompleted(Long paymentId) {
        assertPaymentStatus(paymentId, PaymentStatus.COMPLETE);
        assertOrderStatus(OrderStatus.PAID);
        assertDbStocks(8, 2);
        assertRedisStocks("8", "2");
        assertReservation(paymentId, "CONFIRMED");
    }

    private void assertNoPayment() {
        assertThat(jdbc.queryForObject(
                "select count(*) from payments where order_id = ?",
                Long.class, fixture.orderId())).isZero();
    }

    // 저장했던 객체의 메모리 값이 아니라 DB를 새로 조회하여 검증한다.
    private void assertDbStocks(int expectedA, int expectedB) {
        assertThat(jdbc.queryForObject(
                "select stock from products where product_id = ?",
                Integer.class, fixture.productAId())).isEqualTo(expectedA);
        assertThat(jdbc.queryForObject(
                "select stock from products where product_id = ?",
                Integer.class, fixture.productBId())).isEqualTo(expectedB);
    }

    private void assertPaymentStatus(Long paymentId, PaymentStatus expected) {
        assertThat(jdbc.queryForObject(
                "select pay_status from payments where pay_id = ?",
                String.class, paymentId)).isEqualTo(expected.name());
    }

    private void assertOrderStatus(OrderStatus expected) {
        assertThat(jdbc.queryForObject(
                "select order_status from orders where order_id = ?",
                String.class, fixture.orderId())).isEqualTo(expected.name());
    }

    private void assertRedisStocks(String expectedA, String expectedB) {
        assertThat(redisTemplate.opsForValue().get(stockKey(fixture.productAId())))
                .isEqualTo(expectedA);
        assertThat(redisTemplate.opsForValue().get(stockKey(fixture.productBId())))
                .isEqualTo(expectedB);
    }

    private void assertReservation(Long paymentId, String expectedStatus) {
        assertThat(redisTemplate.opsForHash().get(reservationKey(paymentId), "status"))
                .isEqualTo(expectedStatus);
        assertThat(redisTemplate.opsForHash().get(reservationKey(paymentId), "items"))
                .isEqualTo(fixture.productAId() + ":2," + fixture.productBId() + ":3");
    }

    private void putStock(Long productId, String stock) {
        String key = stockKey(productId);
        redisKeys.add(key);
        redisTemplate.opsForValue().set(key, stock);
    }

    private String stockKey(Long productId) {
        return "inventory:available:" + productId;
    }

    private String reservationKey(Long paymentId) {
        return "reservation:" + paymentId;
    }

    private record Fixture(
            Long buyerId, Long sellerId, Long categoryId,
            Long productAId, Long productBId, Long orderId
    ) {}
}