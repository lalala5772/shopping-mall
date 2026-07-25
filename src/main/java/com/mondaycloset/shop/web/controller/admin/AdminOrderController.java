package com.mondaycloset.shop.web.controller.admin;

import com.mondaycloset.shop.domain.order.OrderStatus;
import com.mondaycloset.shop.service.admin.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminOrderController {

    private static final int PAGE_SIZE = 15;

    private final AdminOrderService adminOrderService;

    @GetMapping("/admin/orders")
    public String list(@RequestParam(required = false) OrderStatus status,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        var orderPage = adminOrderService.getOrderPage(status, PageRequest.of(page, PAGE_SIZE));
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        return "admin/order-list";
    }

    @PostMapping("/admin/orders/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam OrderStatus next,
                                @RequestParam(required = false) String carrierCode,
                                @RequestParam(required = false) String trackingNumber,
                                RedirectAttributes redirectAttributes) {
        adminOrderService.changeStatus(id, next, carrierCode, trackingNumber);
        redirectAttributes.addFlashAttribute("successMessage", "주문 상태가 변경되었습니다.");
        return "redirect:/admin/orders";
    }
}
