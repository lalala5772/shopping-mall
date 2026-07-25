package com.mondaycloset.shop.domain.order;

public enum OrderStatus {
    ORDERED("주문완료"),     // 결제 완료로 간주 - 별도 PG 연동은 확장 범위
    SHIPPING("배송중"),
    DELIVERED("배송완료"),
    CANCELLED("주문취소");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
