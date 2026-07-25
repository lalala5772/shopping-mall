package com.mondaycloset.shop.domain.order;

import com.mondaycloset.shop.domain.common.BaseTimeEntity;
import com.mondaycloset.shop.domain.member.Member;
import com.mondaycloset.shop.global.exception.BusinessException;
import com.mondaycloset.shop.global.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문. 테이블명은 예약어 충돌을 피하기 위해 "orders"를 사용한다(클래스명은 Orders).
 * totalPrice는 주문 확정 시점 스냅샷(비정규화③) - 이후 상품 가격이 바뀌어도 과거 주문 금액은 불변.
 */
@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Orders extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    @Column(name = "receiver_address", nullable = false, length = 255)
    private String receiverAddress;

    /** 관리자가 배송중 전환 시 입력하는 운송장 정보. 택배사 코드는 스마트택배(Sweettracker) t_code 기준. */
    @Column(name = "carrier_code", length = 20)
    private String carrierCode;

    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderItem> orderItems = new ArrayList<>();

    public static Orders create(String orderNumber, Member member, String receiverName,
                                 String receiverPhone, String receiverAddress, List<OrderItem> items) {
        Orders order = new Orders();
        order.orderNumber = orderNumber;
        order.member = member;
        order.receiverName = receiverName;
        order.receiverPhone = receiverPhone;
        order.receiverAddress = receiverAddress;
        order.status = OrderStatus.ORDERED;

        int total = 0;
        for (OrderItem item : items) {
            item.assignOrder(order);
            order.orderItems.add(item);
            total += item.totalPrice();
        }
        order.totalPrice = total;
        return order;
    }

    public boolean isOwnedBy(Member member) {
        return this.member.getId().equals(member.getId());
    }

    /** 관리자 페이지에서 상태 변경. 정방향 흐름만 허용한다(주문완료 -> 배송중 -> 배송완료), 취소는 배송 전에만 가능. */
    public void changeStatus(OrderStatus next) {
        boolean allowed = switch (this.status) {
            case ORDERED -> next == OrderStatus.SHIPPING || next == OrderStatus.CANCELLED;
            case SHIPPING -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS_CHANGE,
                    String.format("%s 상태에서 %s(으)로 변경할 수 없습니다.", this.status, next));
        }
        this.status = next;
    }

    public void assignTracking(String carrierCode, String trackingNumber) {
        this.carrierCode = carrierCode;
        this.trackingNumber = trackingNumber;
    }
}
