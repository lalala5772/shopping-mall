package com.mondaycloset.shop.repository;

import com.mondaycloset.shop.domain.product.Product;
import com.mondaycloset.shop.domain.product.ProductStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatusNot(ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryIdAndStatusNot(Long categoryId, ProductStatus status, Pageable pageable);

    Page<Product> findByNameContainingAndStatusNot(String keyword, ProductStatus status, Pageable pageable);

    // 관리자 목록: HIDDEN 포함 전체 조회
    Page<Product> findAllByOrderByIdDesc(Pageable pageable);

    // 이미지 유사도 검색: 임베딩이 계산된 노출 상품만 후보로 삼는다(카탈로그가 작아 전량 로드 후 코사인 유사도를 계산해도 무리 없음)
    List<Product> findByStatusNotAndImageEmbeddingIsNotNull(ProductStatus status);

    // 임베딩 백필 대상 조회(관리자 전용)
    List<Product> findByImageEmbeddingIsNull();
}
