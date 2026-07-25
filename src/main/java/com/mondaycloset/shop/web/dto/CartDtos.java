package com.mondaycloset.shop.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class CartDtos {

    private CartDtos() {}

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CartItemView {
        private final Long cartItemId;
        private final Long productId;
        private final String productName;
        private final String thumbnailUrl;
        private final int price;
        private final int quantity;
        private final int totalPrice;
        private final boolean purchasable;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CartView {
        private final List<CartItemView> items;
        private final int totalPrice;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddCartRequest {
        @NotNull(message = "상품 정보가 올바르지 않습니다.")
        private Long productId;

        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        private int quantity = 1;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateQuantityRequest {
        @NotNull
        private Long cartItemId;

        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        private int quantity;
    }
}
