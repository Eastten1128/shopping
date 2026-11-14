package com.jdr1128.shopping.domain.payment.service;

import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.order.entity.Order;
import com.jdr1128.shopping.domain.order.entity.OrderProduct;
import com.jdr1128.shopping.domain.order.entity.OrderStatus;
import com.jdr1128.shopping.domain.order.repository.OrderProductRepository;
import com.jdr1128.shopping.domain.order.repository.OrderRepository;
import com.jdr1128.shopping.domain.payment.dto.PaymentRequest;
import com.jdr1128.shopping.domain.payment.dto.PaymentResponse;
import com.jdr1128.shopping.domain.payment.entity.Payment;
import com.jdr1128.shopping.domain.payment.entity.PaymentStatus;
import com.jdr1128.shopping.domain.payment.exception.AlreadyPaidException;
import com.jdr1128.shopping.domain.payment.exception.PaymentCancelException;
import com.jdr1128.shopping.domain.payment.exception.PaymentNotFoundException;
import com.jdr1128.shopping.domain.payment.repository.PaymentRepository;
import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    private static final Long MEMBER_ID = 1L;
    private static final Long ORDER_ID = 10L;
    private static final Long PAY_ID = 100L;

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderProductRepository orderProductRepository;
    @InjectMocks private PaymentService paymentService;

    private Order order;
    private Product product;
    private List<OrderProduct> orderProducts;

    @BeforeEach
    void setUp() {
        Member member = new Member("buyer", "encoded-password", "buyer@test.com", Role.USER);
        ReflectionTestUtils.setField(member, "memberId", MEMBER_ID);
        order = new Order(member, "buyer", "010-1234-5678", "test address", "", "", 60000);
        ReflectionTestUtils.setField(order, "orderId", ORDER_ID);
        Member seller = new Member("seller", "encoded-password", "seller@test.com", Role.SELLER);
        // 주문 생성에서 2개를 이미 차감한 상태: 원래 재고 10개, 현재 재고 8개.
        product = new Product(new Category("category"), seller, "product", 30000, 8, ProductStatus.SELLING);
        orderProducts = List.of(new OrderProduct(order, product, "product", 2, 30000));
    }

    @Test
    @DisplayName("정상 결제는 주문 금액으로 결제를 저장하고 재고를 다시 차감하지 않는다")
    void payOrder_success() {
        givenMyOrder();
        givenSavePayment();

        PaymentResponse response = paymentService.payOrder(MEMBER_ID, ORDER_ID, request(true));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getOrder()).isSameAs(order);
        assertThat(saved.getPayPrice()).isEqualTo(60000);
        assertThat(saved.getPayStatus()).isEqualTo(PaymentStatus.COMPLETE);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.getPayId()).isEqualTo(PAY_ID);
        assertThat(response.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(response.getPayPrice()).isEqualTo(60000);
        assertThat(response.getPayStatus()).isEqualTo(PaymentStatus.COMPLETE);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(product.getStock()).isEqualTo(8);
        verifyNoInteractions(orderProductRepository);
    }

    @Test
    @DisplayName("결제 실패는 실패 기록을 저장하고 주문 상태 변경 및 재고 복구를 수행한다")
    void payOrder_failure() {
        givenMyOrder();
        givenSavePayment();
        when(orderProductRepository.findByOrder_OrderId(ORDER_ID)).thenReturn(orderProducts);

        PaymentResponse response = paymentService.payOrder(MEMBER_ID, ORDER_ID, request(false));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getPayStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(response.getPayStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(product.getStock()).isEqualTo(10);

        // 실패한 결제도 기록이 있으므로 재결제와 재고 중복 복구를 차단한다.
        when(paymentRepository.existsByOrder_OrderId(ORDER_ID)).thenReturn(true);
        assertThatThrownBy(() -> paymentService.payOrder(MEMBER_ID, ORDER_ID, request(false)))
                .isInstanceOf(AlreadyPaidException.class);
        assertThat(product.getStock()).isEqualTo(10);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderProductRepository, times(1)).findByOrder_OrderId(ORDER_ID);
    }

    @Test
    @DisplayName("완료된 결제 취소는 주문도 취소하고 재고를 한 번만 복구한다")
    void cancelPayment_successAndRejectRepeatedCancel() {
        Payment payment = completedPayment();
        when(paymentRepository.findByPayIdAndOrder_Member_MemberId(PAY_ID, MEMBER_ID))
                .thenReturn(Optional.of(payment));
        when(orderProductRepository.findByOrder_OrderId(ORDER_ID)).thenReturn(orderProducts);

        PaymentResponse response = paymentService.cancelPayment(MEMBER_ID, PAY_ID);

        assertThat(payment.getPayStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.getPayStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStock()).isEqualTo(10);

        assertThatThrownBy(() -> paymentService.cancelPayment(MEMBER_ID, PAY_ID))
                .isInstanceOf(PaymentCancelException.class);
        assertThat(product.getStock()).isEqualTo(10);
        verify(orderProductRepository, times(1)).findByOrder_OrderId(ORDER_ID);
    }

    @Test
    @DisplayName("취소된 미결제 주문은 결제할 수 없다")
    void payOrder_rejectCancelledOrder() {
        givenMyOrder();
        order.cancel();

        assertThatThrownBy(() -> paymentService.payOrder(MEMBER_ID, ORDER_ID, request(true)))
                .isInstanceOf(PaymentCancelException.class);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStock()).isEqualTo(8);
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(orderProductRepository);
    }

    @Test
    @DisplayName("결제가 이미 기록된 주문은 추가 결제를 저장하지 않는다")
    void payOrder_rejectExistingPayment() {
        givenMyOrder();
        when(paymentRepository.existsByOrder_OrderId(ORDER_ID)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.payOrder(MEMBER_ID, ORDER_ID, request(true)))
                .isInstanceOf(AlreadyPaidException.class);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ORDERED);
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(orderProductRepository);
    }

    @Test
    @DisplayName("이미 결제 완료된 주문은 중복 결제할 수 없다")
    void payOrder_rejectPaidOrder() {
        givenMyOrder();
        order.pay();

        assertThatThrownBy(() -> paymentService.payOrder(MEMBER_ID, ORDER_ID, request(true)))
                .isInstanceOf(AlreadyPaidException.class);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(orderProductRepository);
    }

    @Test
    @DisplayName("존재하지 않거나 다른 회원의 주문은 결제할 수 없다")
    void payOrder_rejectMissingOrOtherMembersOrder() {
        when(orderRepository.findByOrderIdAndMember_MemberId(ORDER_ID, MEMBER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.payOrder(MEMBER_ID, ORDER_ID, request(true)))
                .isInstanceOf(PaymentNotFoundException.class);

        verifyNoInteractions(paymentRepository, orderProductRepository);
    }

    @Test
    @DisplayName("실패한 결제를 취소해도 재고가 다시 복구되지 않는다")
    void cancelPayment_rejectFailedPayment() {
        Payment payment = new Payment(order, "test-card", "****-1234", 60000);
        payment.fail();
        order.fail();
        product.increaseStock(2);
        when(paymentRepository.findByPayIdAndOrder_Member_MemberId(PAY_ID, MEMBER_ID))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.cancelPayment(MEMBER_ID, PAY_ID))
                .isInstanceOf(PaymentCancelException.class);

        assertThat(payment.getPayStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(product.getStock()).isEqualTo(10);
        verifyNoInteractions(orderProductRepository);
    }

    @Test
    @DisplayName("다른 회원의 결제는 취소할 수 없다")
    void cancelPayment_rejectOtherMembersPayment() {
        when(paymentRepository.findByPayIdAndOrder_Member_MemberId(PAY_ID, MEMBER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.cancelPayment(MEMBER_ID, PAY_ID))
                .isInstanceOf(PaymentNotFoundException.class);

        verifyNoInteractions(orderProductRepository);
    }

    private void givenMyOrder() {
        when(orderRepository.findByOrderIdAndMember_MemberId(ORDER_ID, MEMBER_ID))
                .thenReturn(Optional.of(order));
    }

    private void givenSavePayment() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "payId", PAY_ID);
            return payment;
        });
    }

    private PaymentRequest request(boolean success) {
        PaymentRequest request = new PaymentRequest();
        ReflectionTestUtils.setField(request, "cardCompany", "test-card");
        ReflectionTestUtils.setField(request, "cardNumber", "****-1234");
        ReflectionTestUtils.setField(request, "paymentSuccess", success);
        return request;
    }

    private Payment completedPayment() {
        Payment payment = new Payment(order, "test-card", "****-1234", 60000);
        ReflectionTestUtils.setField(payment, "payId", PAY_ID);
        payment.complete();
        order.pay();
        return payment;
    }
}
