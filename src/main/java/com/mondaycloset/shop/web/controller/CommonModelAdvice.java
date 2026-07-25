package com.mondaycloset.shop.web.controller;

import com.mondaycloset.shop.security.AppUserPrincipal;
import com.mondaycloset.shop.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 헤더의 장바구니 뱃지 등 모든 화면에서 공통으로 필요한 정보를 매 요청마다 채워준다. */
@ControllerAdvice
@RequiredArgsConstructor
public class CommonModelAdvice {

    private final CartService cartService;

    @ModelAttribute("cartCount")
    public long cartCount(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            return 0L;
        }
        return cartService.countItems(principal.getMemberId());
    }
}
