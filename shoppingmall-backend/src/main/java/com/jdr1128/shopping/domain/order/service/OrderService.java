package com.jdr1128.shopping.domain.order.service;

import com.jdr1128.shopping.domain.cart.entity.Cart;
import com.jdr1128.shopping.domain.cart.entity.CartItem;
import com.jdr1128.shopping.domain.cart.repository.CartItemRepository;
import com.jdr1128.shopping.domain.cart.repository.CartRepository;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.exception.MemberNotFoundException;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.domain.order.dto.OrderCreateRequest;
import com.jdr1128.shopping.domain.order.dto.OrderResponse;
import com.jdr1128.shopping.domain.order.entity.Order;
import com.jdr1128.shopping.domain.order.entity.OrderProduct;
import com.jdr1128.shopping.domain.order.entity.OrderStatus;
import com.jdr1128.shopping.domain.order.exception.EmptyCartException;
import com.jdr1128.shopping.domain.order.exception.OrderNotFoundException;
import com.jdr1128.shopping.domain.order.exception.OutOfStockException;
import com.jdr1128.shopping.domain.order.exception.PriceChangedException;
import com.jdr1128.shopping.domain.order.repository.OrderProductRepository;
import com.jdr1128.shopping.domain.order.repository.OrderRepository;
import com.jdr1128.shopping.domain.payment.exception.AlreadyPaidException;
import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly=true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderProductRepository orderProductRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            MemberRepository memberRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderProductRepository = orderProductRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.memberRepository = memberRepository;
    }



    //장바구니 기반 주문 생성
    @Transactional
    public OrderResponse createOrder(
            Long memberId,
            OrderCreateRequest request
    ){
        //사용자 검사
        Member member = memberRepository.findById(memberId)
                //.orElseThrow(()->new IllegalArgumentException("회원을 찾을 수 없습니다."));
                .orElseThrow(MemberNotFoundException::new);

        //사용자의 장바구니 검사
        Cart cart = cartRepository.findByMember_MemberId(memberId)
                .orElseThrow(EmptyCartException::new);

        //장바구니의 모슨 상품들 -> cartItems
        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());

        //장바구니가 내용물이 없으면
        if(cartItems.isEmpty()){
            throw new EmptyCartException();
        }

        //재고검사
        validatePriceAndStock(cartItems);

        //총 가격 계산
        int totalOrderPrice = calculateTotalOrderPrice(cartItems);

        //Order 객체 생성
        Order order = new Order(
                member,
                request.getName(),
                request.getPhoneNumber(),
                request.getAddress1(),
                request.getAddress2(),
                request.getRequestedTerm(),
                totalOrderPrice
        );

        //주문 order DB 저장
        Order savedOrder = orderRepository.save(order);

        //주문상 상품들에 대해서 상품 개수 감소
        List<OrderProduct> orderProducts = cartItems.stream()
                .map(cartItem -> {
                    Product product = cartItem.getProduct();

                    product.decreaseStock(cartItem.getQuantity());

                    return new OrderProduct(
                            savedOrder,
                            product,
                            product.getProductName(),
                            cartItem.getQuantity(),
                            product.getPrice()
                    );
                })
                .toList();
        orderProductRepository.saveAll(orderProducts);

        cartItemRepository.deleteByCart_CartId(cart.getCartId());

        return OrderResponse.of(savedOrder, orderProducts);
    }

    //내 주문 목록 조회
    public List<OrderResponse>getMyOrders(Long memberId){
        List<Order> orders = orderRepository.findByMember_MemberIdOrderByOrderDateDesc(memberId);

        return orders.stream()
                .map(order->{
                    List<OrderProduct> orderProducts =
                            orderProductRepository.findByOrder_OrderId(order.getOrderId());

                    return OrderResponse.of(order, orderProducts);
                })
                .toList();
    }

    //주문 상세 조회
    public OrderResponse getMyOrder(
            Long memberId,
            Long orderId
    ) {
        Order order = orderRepository.findByOrderIdAndMember_MemberId(orderId, memberId)
                .orElseThrow(OrderNotFoundException::new);

        List<OrderProduct> orderProducts =
                orderProductRepository.findByOrder_OrderId(order.getOrderId());

        return OrderResponse.of(order, orderProducts);
    }

    //주문 취소
    @Transactional
    public OrderResponse cancelOrder(
            Long memberId,
            Long orderId
    ) {
        //취소할 주문의 order_id, member_id로 order 갖고옴
        Order order = orderRepository.findByOrderIdAndMember_MemberId(orderId, memberId)
                .orElseThrow(OrderNotFoundException::new);

        //주문한 상품들을 가져옴
        List<OrderProduct> orderProducts = orderProductRepository.findByOrder_OrderId(order.getOrderId());

        //이미 취소된 주문은 재고를 다시 복구하지 않음
        if (order.isCanceled()) {
            return OrderResponse.of(order, orderProducts);
        }

        //ORDERED가 아닌 주문의 취소는 반려됨
        if (order.getOrderStatus() != OrderStatus.ORDERED){
            throw new AlreadyPaidException();
        }

        //주문상 상품들에 대해서 상품 개수 감소
        for (OrderProduct orderProduct : orderProducts){
            orderProduct.getProduct().increaseStock(orderProduct.getAmount());
        }

        //결제가 이미 주문 취소
        order.cancel();

        //order Return
        return OrderResponse.of(order, orderProducts);
    }

    //주문상품 목록 조회


    /*------*/
    public void validatePriceAndStock(List<CartItem> cartItems){
        //장바구니 내 상품들에 대해서
        for (CartItem cartItem : cartItems) {
            //cartItem에 담김 Product 정보 확인
            Product product = cartItem.getProduct();

            // 둘의 값이 다르면, exception
            if(cartItem.getCartPrice() != product.getPrice()){
                throw new PriceChangedException();
            }

            // 상품이 SELLING 상태가 아니면, Exception
            if (product.getProductStatus() != ProductStatus.SELLING) {
                throw new OutOfStockException();
            }

            // 장바구니 재고가 더 많으면, Exception
            if(product.getStock() < cartItem.getQuantity()){
                throw new OutOfStockException();
            }
        }
    }

    private int calculateTotalOrderPrice(List<CartItem> cartItems){
        return cartItems.stream()
                .mapToInt(cartItem -> cartItem.getCartPrice() * cartItem.getQuantity())
                .sum();
    }
}
