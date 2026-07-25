package com.mondaycloset.shop.repository;

import com.mondaycloset.shop.domain.order.OrderStatus;
import com.mondaycloset.shop.domain.order.Orders;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Orders, Long> {

    Optional<Orders> findByOrderNumber(String orderNumber);

    Page<Orders> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    // 관리자 목록: member는 ManyToOne이므로 join fetch를 페이징과 함께 써도 안전하다(컬렉션 fetch가 아님 -> N+1 방지)
    @Query(value = "select o from Orders o join fetch o.member order by o.createdAt desc",
           countQuery = "select count(o) from Orders o")
    Page<Orders> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = "select o from Orders o join fetch o.member where o.status = :status order by o.createdAt desc",
           countQuery = "select count(o) from Orders o where o.status = :status")
    Page<Orders> findByStatusOrderByCreatedAtDesc(@Param("status") OrderStatus status, Pageable pageable);

    @Query("select o from Orders o join fetch o.orderItems where o.id = :orderId")
    Optional<Orders> findByIdWithItems(@Param("orderId") Long orderId);

    // ---------- 관리자 대시보드 집계 ----------

    long countByStatus(OrderStatus status);

    long countByStatusNot(OrderStatus status);

    long countByStatusNotAndCreatedAtAfter(OrderStatus status, LocalDateTime after);

    @Query("select coalesce(sum(o.totalPrice), 0) from Orders o where o.status <> :status")
    long sumTotalPriceByStatusNot(@Param("status") OrderStatus status);

    @Query("select coalesce(sum(o.totalPrice), 0) from Orders o where o.status <> :status and o.createdAt >= :after")
    long sumTotalPriceByStatusNotAndCreatedAtAfter(@Param("status") OrderStatus status, @Param("after") LocalDateTime after);

    @Query("select o.status as status, count(o) as cnt from Orders o group by o.status")
    List<StatusCountRow> countGroupByStatus();

    List<Orders> findByStatusNotAndCreatedAtAfterOrderByCreatedAt(OrderStatus status, LocalDateTime after);

    interface StatusCountRow {
        OrderStatus getStatus();
        long getCnt();
    }
}
