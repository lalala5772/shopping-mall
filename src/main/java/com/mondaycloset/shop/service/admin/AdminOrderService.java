package com.mondaycloset.shop.service.admin;

import com.mondaycloset.shop.domain.order.OrderStatus;
import com.mondaycloset.shop.domain.order.Orders;
import com.mondaycloset.shop.global.exception.BusinessException;
import com.mondaycloset.shop.global.exception.ErrorCode;
import com.mondaycloset.shop.repository.OrderRepository;
import com.mondaycloset.shop.service.OrderService;
import com.mondaycloset.shop.web.dto.OrderDtos.OrderSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private final OrderRepository orderRepository;

    public Page<OrderSummary> getOrderPage(OrderStatus statusFilter, Pageable pageable) {
        Page<Orders> page = (statusFilter == null)
                ? orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                : orderRepository.findByStatusOrderByCreatedAtDesc(statusFilter, pageable);
        return page.map(o -> OrderService.toSummary(o, o.getMember().getName()));
    }

    /**
     * 취소(CANCELLED)로 전환되면 주문 당시 차감했던 재고를 복원한다.
     * 복원하지 않으면 취소가 반복될수록 실제 판매 가능한 재고가 서서히 소실된다.
     * carrierCode/trackingNumber는 배송중 전환 시 함께 입력받는 운송장 정보(선택).
     */
    @Transactional
    public void changeStatus(Long orderId, OrderStatus next, String carrierCode, String trackingNumber) {
        Orders order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        OrderStatus previous = order.getStatus();
        order.changeStatus(next);

        if (next == OrderStatus.CANCELLED && previous != OrderStatus.CANCELLED) {
            OrderService.restoreStock(order);
        }
        if (carrierCode != null && !carrierCode.isBlank() && trackingNumber != null && !trackingNumber.isBlank()) {
            order.assignTracking(carrierCode, trackingNumber);
        }
    }
}
