package com.mondaycloset.shop.web.controller;

import com.mondaycloset.shop.security.AppUserPrincipal;
import com.mondaycloset.shop.service.CartService;
import com.mondaycloset.shop.web.dto.CartDtos.AddCartRequest;
import com.mondaycloset.shop.web.dto.CartDtos.UpdateQuantityRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/cart")
    public String view(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("cart", cartService.getCart(principal.getMemberId()));
        return "cart/list";
    }

    @PostMapping("/cart/items")
    public String add(@AuthenticationPrincipal AppUserPrincipal principal,
                       @Valid @ModelAttribute AddCartRequest request,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "요청한 수량이 올바르지 않습니다.");
            return "redirect:/products/" + request.getProductId();
        }
        cartService.addToCart(principal.getMemberId(), request.getProductId(), request.getQuantity());
        redirectAttributes.addFlashAttribute("successMessage", "장바구니에 담았습니다.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/quantity")
    public String changeQuantity(@AuthenticationPrincipal AppUserPrincipal principal,
                                  @Valid @ModelAttribute UpdateQuantityRequest request,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "수량은 1개 이상이어야 합니다.");
            return "redirect:/cart";
        }
        cartService.changeQuantity(principal.getMemberId(), request.getCartItemId(), request.getQuantity());
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{cartItemId}/delete")
    public String remove(@AuthenticationPrincipal AppUserPrincipal principal,
                          @PathVariable Long cartItemId) {
        cartService.removeItem(principal.getMemberId(), cartItemId);
        return "redirect:/cart";
    }
}
