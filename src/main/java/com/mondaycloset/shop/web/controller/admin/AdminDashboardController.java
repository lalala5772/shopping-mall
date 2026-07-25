package com.mondaycloset.shop.web.controller.admin;

import com.mondaycloset.shop.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("summary", adminDashboardService.getSummary());
        model.addAttribute("statusBreakdown", adminDashboardService.getStatusBreakdown());
        model.addAttribute("dailyTrend", adminDashboardService.getDailyTrend());
        model.addAttribute("topProducts", adminDashboardService.getTopProducts());
        return "admin/dashboard";
    }
}
