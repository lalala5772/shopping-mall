package com.mondaycloset.shop.service;

import com.mondaycloset.shop.domain.cart.CartItem;
import com.mondaycloset.shop.domain.member.Member;
import com.mondaycloset.shop.domain.order.OrderItem;
import com.mondaycloset.shop.domain.order.OrderStatus;
import com.mondaycloset.shop.domain.order.Orders;
import com.mondaycloset.shop.domain.product.Product;
import com.mondaycloset.shop.global.exception.BusinessException;
import com.mondaycloset.shop.global.exception.ErrorCode;
import com.mondaycloset.shop.repository.CartItemRepository;
import com.mondaycloset.shop.repository.CartRepository;
import com.mondaycloset.shop.repository.MemberRepository;
import com.mondaycloset.shop.repository.OrderRepository;
import com.mondaycloset.shop.web.dto.OrderDtos.OrderDetailView;
import com.mondaycloset.shop.web.dto.OrderDtos.OrderForm;
import com.mondaycloset.shop.web.dto.OrderDtos.OrderItemView;
import com.mondaycloset.shop.web.dto.OrderDtos.OrderSummary;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;

    /**
     * 장바구니 전체를 하나의 주문으로 확정한다.
     * 재고 차감(Product.decreaseStock)은 @Version 낙관적 락으로 보호되며,
     * 동시 주문으로 충돌이 발생하면 OptimisticLockException -> GlobalExceptionHandler에서 처리한다.
     */
    @Transactional
    public Long placeOrder(Long memberId, OrderForm form) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        var cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));
        List<CartItem> cartItems = cartItemRepository.findAllByCartIdWithProduct(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_CART);
        }

        List<OrderItem> orderItems = cartItems.stream()
                .map(ci -> {
                    Product product = ci.getProduct();
                    if (!product.isPurchasable()) {
                        throw new BusinessException(ErrorCode.PRODUCT_NOT_PURCHASABLE,
                                product.getName() + "은(는) 현재 구매할 수 없습니다.");
                    }
                    product.decreaseStock(ci.getQuantity());
                    return OrderItem.of(product, ci.getQuantity());
                })
                .toList();

        Orders order = Orders.create(
                generateOrderNumber(),
                member,
                form.getReceiverName(),
                form.getReceiverPhone(),
                form.getReceiverAddress(),
                orderItems
        );
        orderRepository.save(order);

        cartItemRepository.deleteAll(cartItems);

        return order.getId();
    }

    public Page<OrderSummary> getMyOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(o -> toSummary(o, null));
    }

    public OrderDetailView getOrderDetail(Long memberId, Long orderId) {
        Orders order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (!order.isOwnedBy(member) && !member.isAdmin()) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return toDetail(order);
    }

    /**
     * 본인 주문 취소. Orders.changeStatus()가 ORDERED -> CANCELLED만 허용하므로
     * 배송이 시작된(SHIPPING 이후) 주문은 자동으로 거부된다.
     */
    @Transactional
    public void cancelOrder(Long memberId, Long orderId) {
        Orders order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (!order.isOwnedBy(member)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        order.changeStatus(OrderStatus.CANCELLED);
        restoreStock(order);
    }

    /** 취소된 주문의 재고를 되돌린다. 관리자 취소(AdminOrderService)와 로직을 공유한다. */
    public static void restoreStock(Orders order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            if (product != null) {
                product.restoreStock(item.getQuantity());
            }
        }
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(ORDER_NO_DATE);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + datePart + "-" + randomPart;
    }

    public static OrderSummary toSummary(Orders order, String buyerName) {
        return OrderSummary.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .statusLabel(order.getStatus().label())
                .totalPrice(order.getTotalPrice())
                .createdAt(order.getCreatedAt())
                .buyerName(buyerName)
                .carrierCode(order.getCarrierCode())
                .trackingNumber(order.getTrackingNumber())
                .build();
    }

    public static OrderDetailView toDetail(Orders order) {
        List<OrderItemView> items = order.getOrderItems().stream()
                .map(i -> OrderItemView.builder()
                        .productName(i.getProductName())
                        .price(i.getPrice())
                        .quantity(i.getQuantity())
                        .totalPrice(i.totalPrice())
                        .build())
                .toList();
        return OrderDetailView.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .statusLabel(order.getStatus().label())
                .totalPrice(order.getTotalPrice())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddress(order.getReceiverAddress())
                .createdAt(order.getCreatedAt())
                .items(items)
                .carrierCode(order.getCarrierCode())
                .trackingNumber(order.getTrackingNumber())
                .build();
    }
}
