package com.mondaycloset.shop.service;

import com.mondaycloset.shop.domain.cart.Cart;
import com.mondaycloset.shop.domain.cart.CartItem;
import com.mondaycloset.shop.domain.member.Member;
import com.mondaycloset.shop.domain.product.Product;
import com.mondaycloset.shop.global.exception.BusinessException;
import com.mondaycloset.shop.global.exception.ErrorCode;
import com.mondaycloset.shop.repository.CartItemRepository;
import com.mondaycloset.shop.repository.CartRepository;
import com.mondaycloset.shop.repository.MemberRepository;
import com.mondaycloset.shop.repository.ProductRepository;
import com.mondaycloset.shop.web.dto.CartDtos.CartItemView;
import com.mondaycloset.shop.web.dto.CartDtos.CartView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void addToCart(Long memberId, Long productId, int quantity) {
        Cart cart = getOrCreateCart(memberId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isPurchasable()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_PURCHASABLE);
        }

        cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .ifPresentOrElse(
                        existing -> existing.addQuantity(quantity),
                        () -> cartItemRepository.save(new CartItem(cart, product, quantity))
                );
    }

    /**
     * 순수 조회. 카트가 아직 없으면(예: 시드 데이터로 생성된 계정) 생성하지 않고 빈 장바구니로 응답한다.
     * 읽기 전용 트랜잭션(@Transactional(readOnly = true))에서 실행되므로 여기서 절대 INSERT를 시도하면 안 된다
     * (MySQL은 read-only 트랜잭션에서의 쓰기를 드라이버 레벨에서 거부한다).
     */
    public CartView getCart(Long memberId) {
        return cartRepository.findByMemberId(memberId)
                .map(cart -> {
                    List<CartItemView> items = cartItemRepository.findAllByCartIdWithProduct(cart.getId()).stream()
                            .map(this::toView)
                            .toList();
                    int totalPrice = items.stream().mapToInt(CartItemView::getTotalPrice).sum();
                    return CartView.builder().items(items).totalPrice(totalPrice).build();
                })
                .orElseGet(() -> CartView.builder().items(List.of()).totalPrice(0).build());
    }

    /** 헤더 뱃지용 카운트. getCart와 동일한 이유로 카트 생성 없이 0을 반환한다. */
    public long countItems(Long memberId) {
        return cartRepository.findByMemberId(memberId)
                .map(cart -> cartItemRepository.countByCartId(cart.getId()))
                .orElse(0L);
    }

    @Transactional
    public void changeQuantity(Long memberId, Long cartItemId, int quantity) {
        CartItem item = getOwnedCartItem(memberId, cartItemId);
        item.changeQuantity(quantity);
    }

    @Transactional
    public void removeItem(Long memberId, Long cartItemId) {
        CartItem item = getOwnedCartItem(memberId, cartItemId);
        cartItemRepository.delete(item);
    }

    /** 주문 완료 후 장바구니 비우기 */
    @Transactional
    public void clear(Long memberId, List<Long> productIds) {
        Cart cart = getOrCreateCart(memberId);
        for (Long productId : productIds) {
            cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                    .ifPresent(cartItemRepository::delete);
        }
    }

    private CartItem getOwnedCartItem(Long memberId, Long cartItemId) {
        Cart cart = getOrCreateCart(memberId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return item;
    }

    private Cart getOrCreateCart(Long memberId) {
        return cartRepository.findByMemberId(memberId).orElseGet(() -> {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            return cartRepository.save(new Cart(member));
        });
    }

    private CartItemView toView(CartItem item) {
        Product product = item.getProduct();
        return CartItemView.builder()
                .cartItemId(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .thumbnailUrl(product.getThumbnailUrl())
                .price(product.getPrice())
                .quantity(item.getQuantity())
                .totalPrice(item.totalPrice())
                .purchasable(product.isPurchasable())
                .build();
    }
}
