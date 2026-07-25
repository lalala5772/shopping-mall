package com.mondaycloset.shop.repository;

import com.mondaycloset.shop.domain.order.OrderItem;
import com.mondaycloset.shop.domain.order.OrderStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    /** 상품별 누적 판매 수량/매출 - 관리자 대시보드 인기상품 집계. productName은 주문 시점 스냅샷이라 상품이 삭제돼도 집계가 유지된다. */
    @Query("""
            select oi.productName as productName, sum(oi.quantity) as totalQuantity, sum(oi.price * oi.quantity) as totalRevenue
            from OrderItem oi
            where oi.order.status <> :excludedStatus
            group by oi.productName
            order by sum(oi.quantity) desc
            """)
    List<ProductSalesRow> findTopSellingProducts(@Param("excludedStatus") OrderStatus excludedStatus, Pageable pageable);

    interface ProductSalesRow {
        String getProductName();
        long getTotalQuantity();
        long getTotalRevenue();
    }
}
