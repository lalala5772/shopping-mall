package com.mondaycloset.shop.web.controller;

import com.mondaycloset.shop.security.AppUserPrincipal;
import com.mondaycloset.shop.service.CartService;
import com.mondaycloset.shop.service.OrderService;
import com.mondaycloset.shop.web.dto.OrderDtos.OrderForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    @Value("${app.juso.confirm-key}")
    private String jusoConfirmKey;

    @GetMapping("/orders/checkout")
    public String checkoutForm(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        var cart = cartService.getCart(principal.getMemberId());
        model.addAttribute("cart", cart);
        if (!model.containsAttribute("orderForm")) {
            model.addAttribute("orderForm", new OrderForm());
        }
        model.addAttribute("jusoConfirmKey", jusoConfirmKey);
        return "order/checkout";
    }

    @PostMapping("/orders")
    public String placeOrder(@AuthenticationPrincipal AppUserPrincipal principal,
                              @Valid @ModelAttribute("orderForm") OrderForm form,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("cart", cartService.getCart(principal.getMemberId()));
            model.addAttribute("jusoConfirmKey", jusoConfirmKey);
            return "order/checkout";
        }
        Long orderId = orderService.placeOrder(principal.getMemberId(), form);
        return "redirect:/orders/" + orderId + "/complete";
    }

    @GetMapping("/orders/{id}/complete")
    public String complete(@AuthenticationPrincipal AppUserPrincipal principal,
                            @PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrderDetail(principal.getMemberId(), id));
        return "order/complete";
    }
}
