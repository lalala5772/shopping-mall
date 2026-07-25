package com.mondaycloset.shop.service.admin;

import com.mondaycloset.shop.domain.category.Category;
import com.mondaycloset.shop.domain.product.Product;
import com.mondaycloset.shop.global.exception.BusinessException;
import com.mondaycloset.shop.global.exception.ErrorCode;
import com.mondaycloset.shop.repository.CategoryRepository;
import com.mondaycloset.shop.repository.ProductRepository;
import com.mondaycloset.shop.service.BedrockImageEmbeddingService;
import com.mondaycloset.shop.web.dto.ProductDtos.AdminProductRow;
import com.mondaycloset.shop.web.dto.ProductDtos.ProductForm;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** 관리자 상품 관리. 실제 DELETE 대신 HIDDEN 상태 전환(소프트 삭제)을 사용해 주문/장바구니 참조 무결성을 지킨다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BedrockImageEmbeddingService embeddingService;
    private final RestTemplate restTemplate;

    public Page<AdminProductRow> getProductPage(Pageable pageable) {
        return productRepository.findAllByOrderByIdDesc(pageable).map(p -> AdminProductRow.builder()
                .id(p.getId())
                .categoryName(p.getCategory().getName())
                .name(p.getName())
                .price(p.getPrice())
                .stockQuantity(p.getStockQuantity())
                .statusLabel(p.getStatus().label())
                .hidden(p.isHidden())
                .build());
    }

    public ProductForm getProductForm(Long productId) {
        Product product = findProduct(productId);
        return ProductForm.builder()
                .id(product.getId())
                .categoryId(product.getCategory().getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .thumbnailUrl(product.getThumbnailUrl())
                .stockQuantity(product.getStockQuantity())
                .build();
    }

    @Transactional
    public Long create(ProductForm form) {
        Category category = findCategory(form.getCategoryId());
        Product product = Product.builder()
                .category(category)
                .name(form.getName())
                .price(form.getPrice())
                .description(form.getDescription())
                .thumbnailUrl(form.getThumbnailUrl())
                .stockQuantity(form.getStockQuantity())
                .build();
        productRepository.save(product);
        return product.getId();
    }

    @Transactional
    public void update(Long productId, ProductForm form) {
        Product product = findProduct(productId);
        Category category = findCategory(form.getCategoryId());
        product.changeInfo(category, form.getName(), form.getPrice(), form.getDescription(),
                form.getThumbnailUrl(), form.getStockQuantity());
    }

    /**
     * 썸네일 이미지를 내려받아 Bedrock 임베딩을 계산하고 저장한다.
     * create()/update()와 별도 트랜잭션으로 분리했다 - 이미지 다운로드 + Bedrock 호출은 느린 외부 I/O라서
     * 같은 트랜잭션 안에 있으면 그동안 DB 커넥션을 계속 붙잡게 된다. Bedrock이 꺼져있거나 실패해도
     * 상품 저장 자체는 이미 끝난 뒤이므로 상품 등록/수정은 항상 성공한다(임베딩은 나중에 백필 가능).
     */
    @Transactional
    public void embedProductImage(Long productId) {
        if (!embeddingService.isEnabled()) {
            return;
        }
        downloadAndEmbed(findProduct(productId));
    }

    /**
     * 임베딩이 없는 상품(과거 데이터, 시드 데이터 등)을 한 번에 채워 넣는다.
     * 상품 수가 적어(수십 개) 배치 전체를 하나의 트랜잭션으로 처리해도 커넥션을 오래 붙잡지 않는다 -
     * 카탈로그가 커지면 페이지 단위로 나눠 처리하도록 재검토가 필요하다.
     */
    @Transactional
    public int backfillEmbeddings() {
        if (!embeddingService.isEnabled()) {
            return 0;
        }
        int succeeded = 0;
        for (Product product : productRepository.findByImageEmbeddingIsNull()) {
            if (downloadAndEmbed(product)) {
                succeeded++;
            }
        }
        return succeeded;
    }

    /** 현재 트랜잭션에서 관리되고 있는 product 엔티티를 그 자리에서 갱신한다(별도 조회 없음). */
    private boolean downloadAndEmbed(Product product) {
        String thumbnailUrl = product.getThumbnailUrl();
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
            return false;
        }
        try {
            byte[] imageBytes = restTemplate.getForObject(thumbnailUrl, byte[].class);
            if (imageBytes == null) {
                return false;
            }
            Optional<float[]> vector = embeddingService.embed(imageBytes, BedrockImageEmbeddingService.Purpose.INDEX);
            vector.ifPresent(v -> product.updateImageEmbedding(embeddingService.toJson(v)));
            return vector.isPresent();
        } catch (RestClientException e) {
            log.warn("[AdminProduct] 썸네일 다운로드 실패 productId={}, url={}, error={}",
                    product.getId(), thumbnailUrl, e.getMessage());
            return false;
        }
    }

    @Transactional
    public void hide(Long productId) {
        findProduct(productId).hide();
    }

    @Transactional
    public void reactivate(Long productId) {
        findProduct(productId).reactivate();
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "존재하지 않는 카테고리입니다."));
    }
}
