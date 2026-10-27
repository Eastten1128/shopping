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
import com.jdr1128.shopping.domain.order.exception.EmptyCartException;
import com.jdr1128.shopping.domain.order.exception.OrderNotFoundException;
import com.jdr1128.shopping.domain.order.exception.OutOfStockException;
import com.jdr1128.shopping.domain.order.exception.PriceChangedException;
import com.jdr1128.shopping.domain.order.repository.OrderProductRepository;
import com.jdr1128.shopping.domain.order.repository.OrderRepository;
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
        Member member = memberRepository.findById(memberId)
                //.orElseThrow(()->new IllegalArgumentException("회원을 찾을 수 없습니다."));
                .orElseThrow(MemberNotFoundException::new);

        Cart cart = cartRepository.findByMember_MemberId(memberId)
                .orElseThrow(EmptyCartException::new);

        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());

        if(cartItems.isEmpty()){
            throw new EmptyCartException();
        }

        validatePriceANdStock(cartItems);

        int totalOrderPrice = calculateTotalOrderPrice(cartItems);

        Order order = new Order(
                member,
                request.getName(),
                request.getPhoneNumber(),
                request.getAddress1(),
                request.getAddress2(),
                request.getRequestedTerm(),
                totalOrderPrice
        );

        Order savedOrder = orderRepository.save(order);

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
        Order order = orderRepository.findByOrderIdAndMember_MemberId(orderId, memberId)
                .orElseThrow(OrderNotFoundException::new);

        order.cancel();

        List<OrderProduct> orderProducts =
                orderProductRepository.findByOrder_OrderId(order.getOrderId());

        return OrderResponse.of(order, orderProducts);
    }

    //주문상품 목록 조회


    /*------*/
    public void validatePriceANdStock(List<CartItem> cartItems){
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if(cartItem.getCartPrice() != product.getPrice()){
                throw new PriceChangedException();
            }

            if (product.getProductStatus() != ProductStatus.SELLING) {
                throw new OutOfStockException();
            }

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
