package com.mondaycloset.shop.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ProductDtos {

    private ProductDtos() {}

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ProductListItem {
        private final Long id;
        private final String name;
        private final int price;
        private final String thumbnailUrl;
        private final boolean soldOut;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ProductDetailResponse {
        private final Long id;
        private final String categoryName;
        private final String name;
        private final int price;
        private final String description;
        private final List<String> images;
        private final int stockQuantity;
        private final boolean purchasable;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AdminProductRow {
        private final Long id;
        private final String categoryName;
        private final String name;
        private final int price;
        private final int stockQuantity;
        private final String statusLabel;
        private final boolean hidden;
    }

    /** 관리자 상품 등록/수정 폼 바인딩 객체. id가 있으면 수정, 없으면 신규 등록. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductForm {
        private Long id;

        @NotNull(message = "카테고리를 선택해 주세요.")
        private Long categoryId;

        @NotBlank(message = "상품명을 입력해 주세요.")
        private String name;

        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        private int price;

        private String description;

        private String thumbnailUrl;

        @Min(value = 0, message = "재고는 0개 이상이어야 합니다.")
        private int stockQuantity;

        public static ProductForm empty() {
            return ProductForm.builder().build();
        }
    }
}
