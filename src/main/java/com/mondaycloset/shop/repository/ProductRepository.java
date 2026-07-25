package com.mondaycloset.shop.repository;

import com.mondaycloset.shop.domain.product.Product;
import com.mondaycloset.shop.domain.product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatusNot(ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryIdAndStatusNot(Long categoryId, ProductStatus status, Pageable pageable);

    Page<Product> findByNameContainingAndStatusNot(String keyword, ProductStatus status, Pageable pageable);

    // 관리자 목록: HIDDEN 포함 전체 조회
    Page<Product> findAllByOrderByIdDesc(Pageable pageable);
}
