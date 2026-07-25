package com.mondaycloset.shop.domain.order;

import com.mondaycloset.shop.domain.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 상품 라인.
 * productName/price는 주문 시점 스냅샷(비정규화②) - 이후 상품 가격 변경/삭제와 무관하게 주문 이력은 불변으로 유지된다.
 */
@Getter
@Entity
@Table(name = "order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int quantity;

    public static OrderItem of(Product product, int quantity) {
        OrderItem item = new OrderItem();
        item.product = product;
        item.productName = product.getName();
        item.price = product.getPrice();
        item.quantity = quantity;
        return item;
    }

    void assignOrder(Orders order) {
        this.order = order;
    }

    public int totalPrice() {
        return this.price * this.quantity;
    }
}
