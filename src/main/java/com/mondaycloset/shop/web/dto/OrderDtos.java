package com.mondaycloset.shop.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class OrderDtos {

    private OrderDtos() {}

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderForm {
        @NotBlank(message = "받는 분 성함을 입력해 주세요.")
        @Size(max = 50, message = "받는 분 성함은 50자를 넘을 수 없습니다.")
        private String receiverName;

        @NotBlank(message = "연락처를 입력해 주세요.")
        @Size(max = 20, message = "연락처는 20자를 넘을 수 없습니다.")
        private String receiverPhone;

        @NotBlank(message = "배송지 주소를 입력해 주세요.")
        @Size(max = 255, message = "배송지 주소는 255자를 넘을 수 없습니다.")
        private String receiverAddress;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderItemView {
        private final String productName;
        private final int price;
        private final int quantity;
        private final int totalPrice;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderSummary {
        private final Long orderId;
        private final String orderNumber;
        private final String status;
        private final String statusLabel;
        private final int totalPrice;
        private final LocalDateTime createdAt;
        /** 관리자 주문 목록에서만 사용(구매자 표시). 마이페이지 목록에서는 null. */
        private final String buyerName;
        private final String carrierCode;
        private final String trackingNumber;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderDetailView {
        private final Long orderId;
        private final String orderNumber;
        private final String status;
        private final String statusLabel;
        private final int totalPrice;
        private final String receiverName;
        private final String receiverPhone;
        private final String receiverAddress;
        private final LocalDateTime createdAt;
        private final List<OrderItemView> items;
        private final String carrierCode;
        private final String trackingNumber;
    }
}
