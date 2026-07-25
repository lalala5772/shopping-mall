package com.mondaycloset.shop.service;

import com.mondaycloset.shop.domain.product.Product;
import com.mondaycloset.shop.domain.product.ProductImage;
import com.mondaycloset.shop.domain.product.ProductStatus;
import com.mondaycloset.shop.global.exception.BusinessException;
import com.mondaycloset.shop.global.exception.ErrorCode;
import com.mondaycloset.shop.repository.ProductImageRepository;
import com.mondaycloset.shop.repository.ProductRepository;
import com.mondaycloset.shop.web.dto.ProductDtos.ProductDetailResponse;
import com.mondaycloset.shop.web.dto.ProductDtos.ProductListItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    /** 목록 조회: 카테고리 필터/검색어 중 활성화된 조건만 적용. HIDDEN 상품은 고객 화면에서 제외한다. */
    public Page<ProductListItem> getProductList(Long categoryId, String keyword, Pageable pageable) {
        Page<Product> page;
        if (StringUtils.hasText(keyword)) {
            page = productRepository.findByNameContainingAndStatusNot(keyword, ProductStatus.HIDDEN, pageable);
        } else if (categoryId != null) {
            page = productRepository.findByCategoryIdAndStatusNot(categoryId, ProductStatus.HIDDEN, pageable);
        } else {
            page = productRepository.findByStatusNot(ProductStatus.HIDDEN, pageable);
        }
        return page.map(this::toListItem);
    }

    @Transactional
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (product.getStatus() == ProductStatus.HIDDEN) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        product.increaseViewCount();

        List<String> images = productImageRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                .map(ProductImage::getImageUrl)
                .toList();
        if (images.isEmpty() && product.getThumbnailUrl() != null) {
            images = List.of(product.getThumbnailUrl());
        }

        return ProductDetailResponse.builder()
                .id(product.getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .images(images)
                .stockQuantity(product.getStockQuantity())
                .purchasable(product.isPurchasable())
                .build();
    }

    private ProductListItem toListItem(Product product) {
        return ProductListItem.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .thumbnailUrl(product.getThumbnailUrl())
                .soldOut(!product.isPurchasable())
                .build();
    }
}
