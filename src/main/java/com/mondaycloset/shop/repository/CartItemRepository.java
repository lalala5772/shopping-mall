package com.mondaycloset.shop.repository;

import com.mondaycloset.shop.domain.cart.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    // fetch join으로 N+1 방지 (장바구니 화면에서 상품 정보까지 한 번에 조회)
    @Query("select ci from CartItem ci join fetch ci.product p where ci.cart.id = :cartId order by ci.id asc")
    List<CartItem> findAllByCartIdWithProduct(@Param("cartId") Long cartId);

    long countByCartId(Long cartId);
}
