package com.mondaycloset.shop.web.dto;

import lombok.Builder;
import lombok.Getter;

public class DashboardDtos {

    private DashboardDtos() {}

    @Getter
    @Builder
    public static class Summary {
        private final long totalOrders;
        private final long totalRevenue;
        private final long todayOrders;
        private final long todayRevenue;
        private final long averageOrderValue;
        private final long cancelledOrders;
    }

    @Getter
    @Builder
    public static class StatusCount {
        private final String status;
        private final String label;
        private final long count;
    }

    @Getter
    @Builder
    public static class DailySales {
        private final String date;
        private final long revenue;
        private final long orderCount;
    }

    @Getter
    @Builder
    public static class TopProduct {
        private final String productName;
        private final long quantity;
        private final long revenue;
    }
}
