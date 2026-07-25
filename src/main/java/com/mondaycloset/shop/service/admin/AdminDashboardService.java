package com.mondaycloset.shop.service.admin;

import com.mondaycloset.shop.domain.order.OrderStatus;
import com.mondaycloset.shop.domain.order.Orders;
import com.mondaycloset.shop.repository.OrderItemRepository;
import com.mondaycloset.shop.repository.OrderRepository;
import com.mondaycloset.shop.web.dto.DashboardDtos.DailySales;
import com.mondaycloset.shop.web.dto.DashboardDtos.StatusCount;
import com.mondaycloset.shop.web.dto.DashboardDtos.Summary;
import com.mondaycloset.shop.web.dto.DashboardDtos.TopProduct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 대시보드용 집계. 취소된 주문은 매출/판매량 집계에서 전부 제외한다(취소는 실매출이 아니므로). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int TREND_DAYS = 14;
    private static final int TOP_PRODUCT_LIMIT = 5;
    private static final DateTimeFormatter TREND_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public Summary getSummary() {
        var todayStart = LocalDate.now().atStartOfDay();
        long totalOrders = orderRepository.countByStatusNot(OrderStatus.CANCELLED);
        long totalRevenue = orderRepository.sumTotalPriceByStatusNot(OrderStatus.CANCELLED);
        long todayOrders = orderRepository.countByStatusNotAndCreatedAtAfter(OrderStatus.CANCELLED, todayStart);
        long todayRevenue = orderRepository.sumTotalPriceByStatusNotAndCreatedAtAfter(OrderStatus.CANCELLED, todayStart);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long averageOrderValue = totalOrders == 0 ? 0 : totalRevenue / totalOrders;

        return Summary.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .todayOrders(todayOrders)
                .todayRevenue(todayRevenue)
                .averageOrderValue(averageOrderValue)
                .cancelledOrders(cancelledOrders)
                .build();
    }

    public List<StatusCount> getStatusBreakdown() {
        return orderRepository.countGroupByStatus().stream()
                .map(row -> StatusCount.builder()
                        .status(row.getStatus().name())
                        .label(row.getStatus().label())
                        .count(row.getCnt())
                        .build())
                .toList();
    }

    /** 최근 N일 매출 추이. 주문이 없는 날짜도 0으로 채워서 그래프가 끊기지 않게 한다. */
    public List<DailySales> getDailyTrend() {
        var since = LocalDate.now().minusDays(TREND_DAYS - 1L).atStartOfDay();
        List<Orders> orders = orderRepository.findByStatusNotAndCreatedAtAfterOrderByCreatedAt(OrderStatus.CANCELLED, since);
        Map<LocalDate, List<Orders>> byDate = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));

        List<DailySales> result = new ArrayList<>();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            List<Orders> dayOrders = byDate.getOrDefault(date, List.of());
            long revenue = dayOrders.stream().mapToLong(Orders::getTotalPrice).sum();
            result.add(DailySales.builder()
                    .date(date.format(TREND_LABEL))
                    .revenue(revenue)
                    .orderCount(dayOrders.size())
                    .build());
        }
        return result;
    }

    public List<TopProduct> getTopProducts() {
        return orderItemRepository.findTopSellingProducts(OrderStatus.CANCELLED, PageRequest.of(0, TOP_PRODUCT_LIMIT)).stream()
                .map(row -> TopProduct.builder()
                        .productName(row.getProductName())
                        .quantity(row.getTotalQuantity())
                        .revenue(row.getTotalRevenue())
                        .build())
                .toList();
    }
}
