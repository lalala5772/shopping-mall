package com.mondaycloset.shop.service.admin;

import com.mondaycloset.shop.domain.category.Category;
import com.mondaycloset.shop.domain.product.Product;
import com.mondaycloset.shop.global.exception.BusinessException;
import com.mondaycloset.shop.global.exception.ErrorCode;
import com.mondaycloset.shop.repository.CategoryRepository;
import com.mondaycloset.shop.repository.ProductRepository;
import com.mondaycloset.shop.web.dto.ProductDtos.AdminProductRow;
import com.mondaycloset.shop.web.dto.ProductDtos.ProductForm;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 상품 관리. 실제 DELETE 대신 HIDDEN 상태 전환(소프트 삭제)을 사용해 주문/장바구니 참조 무결성을 지킨다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

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
