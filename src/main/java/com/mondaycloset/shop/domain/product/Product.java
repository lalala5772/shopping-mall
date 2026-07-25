package com.mondaycloset.shop.domain.product;

import com.mondaycloset.shop.domain.category.Category;
import com.mondaycloset.shop.domain.common.BaseTimeEntity;
import com.mondaycloset.shop.global.exception.BusinessException;
import com.mondaycloset.shop.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품.
 * - thumbnail_url 은 의도적 비정규화 컬럼: 목록 조회에서 product_image 조인을 피하기 위함(발표자료 4p 비정규화①)
 * - version 은 낙관적 락 컬럼: 동시 주문 시 재고 차감 충돌을 감지한다
 */
@Getter
@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int price;

    @Lob
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Version
    @Column(nullable = false)
    private Long version;

    /** AWS Bedrock Amazon Nova Multimodal Embeddings 결과(JSON 배열 문자열). 이미지 유사도 검색에만 쓰인다. */
    @Lob
    @Column(name = "image_embedding")
    private String imageEmbedding;

    @Builder
    private Product(Category category, String name, int price, String description,
                     String thumbnailUrl, int stockQuantity) {
        this.category = category;
        this.name = name;
        this.price = price;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.stockQuantity = stockQuantity;
        this.status = stockQuantity > 0 ? ProductStatus.ON_SALE : ProductStatus.SOLD_OUT;
        this.viewCount = 0;
    }

    public void changeInfo(Category category, String name, int price, String description,
                            String thumbnailUrl, int stockQuantity) {
        this.category = category;
        this.name = name;
        this.price = price;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.stockQuantity = stockQuantity;
        refreshStatusByStock();
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    /** 이미지 유사도 검색용 임베딩 갱신. 상품 저장과는 별도 트랜잭션에서 계산 후 호출된다(외부 API 호출을 상품 저장 트랜잭션 밖으로 분리). */
    public void updateImageEmbedding(String imageEmbedding) {
        this.imageEmbedding = imageEmbedding;
    }

    /** 주문 시 재고 차감. @Version 낙관적 락으로 동시 주문에 의한 초과 판매를 방지한다. */
    public void decreaseStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
        this.stockQuantity -= quantity;
        refreshStatusByStock();
    }

    public void restoreStock(int quantity) {
        this.stockQuantity += quantity;
        refreshStatusByStock();
    }

    private void refreshStatusByStock() {
        if (this.status == ProductStatus.HIDDEN) {
            return;
        }
        this.status = this.stockQuantity > 0 ? ProductStatus.ON_SALE : ProductStatus.SOLD_OUT;
    }

    public void hide() {
        this.status = ProductStatus.HIDDEN;
    }

    /** 숨김 처리된 상품을 다시 노출한다. 재고 상태에 맞춰 ON_SALE/SOLD_OUT을 재계산한다. */
    public void reactivate() {
        this.status = this.stockQuantity > 0 ? ProductStatus.ON_SALE : ProductStatus.SOLD_OUT;
    }

    public boolean isHidden() {
        return this.status == ProductStatus.HIDDEN;
    }

    public boolean isPurchasable() {
        return this.status == ProductStatus.ON_SALE && this.stockQuantity > 0;
    }
}
