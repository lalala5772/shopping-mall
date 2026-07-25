package com.mondaycloset.shop.web.controller;

import com.mondaycloset.shop.security.AppUserPrincipal;
import com.mondaycloset.shop.service.DeliveryTrackingService;
import com.mondaycloset.shop.service.MemberService;
import com.mondaycloset.shop.service.OrderService;
import com.mondaycloset.shop.web.dto.OrderDtos.OrderDetailView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private static final int PAGE_SIZE = 10;

    private final OrderService orderService;
    private final MemberService memberService;
    private final DeliveryTrackingService deliveryTrackingService;

    @Value("${app.juso.confirm-key}")
    private String jusoConfirmKey;

    @GetMapping("/mypage/orders")
    public String orders(@AuthenticationPrincipal AppUserPrincipal principal,
                          @RequestParam(defaultValue = "0") int page, Model model) {
        var orderPage = orderService.getMyOrders(principal.getMemberId(), PageRequest.of(page, PAGE_SIZE));
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        return "mypage/orders";
    }

    @GetMapping("/mypage/orders/{id}")
    public String orderDetail(@AuthenticationPrincipal AppUserPrincipal principal,
                               @PathVariable Long id, Model model) {
        OrderDetailView order = orderService.getOrderDetail(principal.getMemberId(), id);
        model.addAttribute("order", order);
        model.addAttribute("tracking",
                deliveryTrackingService.track(order.getCarrierCode(), order.getTrackingNumber()));
        return "mypage/order-detail";
    }

    @PostMapping("/mypage/orders/{id}/cancel")
    public String cancelOrder(@AuthenticationPrincipal AppUserPrincipal principal,
                               @PathVariable Long id, RedirectAttributes redirectAttributes) {
        orderService.cancelOrder(principal.getMemberId(), id);
        redirectAttributes.addFlashAttribute("successMessage", "주문이 취소되었습니다.");
        return "redirect:/mypage/orders/" + id;
    }

    @GetMapping("/mypage/info")
    public String info(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("member", memberService.getMemberInfo(principal.getMemberId()));
        model.addAttribute("jusoConfirmKey", jusoConfirmKey);
        return "mypage/info";
    }

    @PostMapping("/mypage/info")
    public String updateInfo(@AuthenticationPrincipal AppUserPrincipal principal,
                              @RequestParam String name,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String address,
                              RedirectAttributes redirectAttributes) {
        memberService.updateProfile(principal.getMemberId(), name, phone, address);
        redirectAttributes.addFlashAttribute("successMessage", "회원정보가 수정되었습니다.");
        return "redirect:/mypage/info";
    }
}
