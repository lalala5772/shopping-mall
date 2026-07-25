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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final int IMAGE_SEARCH_RESULT_LIMIT = 12;
    // Titan 임베딩은 코사인 유사도가 대체로 높은 값에 몰리므로, 이 값 미만은 "유사하지 않음"으로 보고 제외한다.
    private static final double IMAGE_SEARCH_MIN_SIMILARITY = 0.3;

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BedrockImageEmbeddingService embeddingService;

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

    /**
     * 업로드된 이미지와 시각적으로 유사한 상품을 찾는다.
     * 카탈로그가 작아 임베딩이 있는 상품 전체를 로드해 코사인 유사도를 계산하는 방식(별도 벡터DB 불필요) -
     * 상품 수가 수천 단위로 늘어나면 이 방식은 재검토가 필요하다.
     */
    public List<ProductListItem> searchBySimilarImage(byte[] queryImageBytes) {
        Optional<float[]> queryVector = embeddingService.embed(queryImageBytes, BedrockImageEmbeddingService.Purpose.QUERY);
        if (queryVector.isEmpty()) {
            return List.of();
        }
        return productRepository.findByStatusNotAndImageEmbeddingIsNotNull(ProductStatus.HIDDEN).stream()
                .map(product -> embeddingService.fromJson(product.getImageEmbedding())
                        .map(vector -> Map.entry(product, BedrockImageEmbeddingService.cosineSimilarity(queryVector.get(), vector))))
                .flatMap(Optional::stream)
                .filter(entry -> entry.getValue() >= IMAGE_SEARCH_MIN_SIMILARITY)
                .sorted(Comparator.comparingDouble((Map.Entry<Product, Double> e) -> e.getValue()).reversed())
                .limit(IMAGE_SEARCH_RESULT_LIMIT)
                .map(entry -> toListItem(entry.getKey()))
                .toList();
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
