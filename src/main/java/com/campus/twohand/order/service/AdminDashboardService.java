package com.campus.twohand.order.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.feedback.repo.FeedbackRepository;
import com.campus.twohand.order.repo.OrdersRepository;
import com.campus.twohand.product.repo.ProductRepository;
import com.campus.twohand.report.repo.ReportRepository;
import com.campus.twohand.user.repo.SysUserRepository;
import com.campus.twohand.verify.repo.StudentVerifyRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    private static final String USER_ROLE = "USER";
    private static final String PRODUCT_AUDIT_PENDING = "PENDING";
    private static final String PRODUCT_AUDIT_APPROVED = "APPROVED";
    private static final String PRODUCT_STATUS_ON = "ON";
    private static final String VERIFY_PENDING = "PENDING";
    private static final String REPORT_PENDING = "PENDING";
    private static final String FEEDBACK_OPEN = "OPEN";
    private static final String ORDER_STATUS_UNPAID = "UNPAID";
    private static final String ORDER_STATUS_PAID = "PAID";
    private static final String ORDER_STATUS_FINISHED = "FINISHED";
    private static final String ORDER_STATUS_CANCELLED = "CANCELLED";

    private final OrdersRepository ordersRepository;
    private final ProductRepository productRepository;
    private final StudentVerifyRepository studentVerifyRepository;
    private final ReportRepository reportRepository;
    private final FeedbackRepository feedbackRepository;
    private final SysUserRepository sysUserRepository;
    private final SessionAuthSupport sessionAuthSupport;

    public AdminDashboardService(OrdersRepository ordersRepository,
                                 ProductRepository productRepository,
                                 StudentVerifyRepository studentVerifyRepository,
                                 ReportRepository reportRepository,
                                 FeedbackRepository feedbackRepository,
                                 SysUserRepository sysUserRepository,
                                 SessionAuthSupport sessionAuthSupport) {
        this.ordersRepository = ordersRepository;
        this.productRepository = productRepository;
        this.studentVerifyRepository = studentVerifyRepository;
        this.reportRepository = reportRepository;
        this.feedbackRepository = feedbackRepository;
        this.sysUserRepository = sysUserRepository;
        this.sessionAuthSupport = sessionAuthSupport;
    }

    public ApiResp<?> stats(HttpServletRequest request) {
        sessionAuthSupport.requireAdminId(request);

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        Map<String, Object> data = new HashMap<>();
        data.put("userCount", sysUserRepository.countByRole(USER_ROLE));
        data.put("onSaleProductCount", productRepository.countOnSaleForDashboard());
        data.put("pendingProductCount", productRepository.countByAuditStatus(PRODUCT_AUDIT_PENDING));
        data.put("pendingVerifyCount", studentVerifyRepository.countByStatus(VERIFY_PENDING));
        data.put("pendingReportCount", reportRepository.countByStatus(REPORT_PENDING));
        data.put("pendingFeedbackCount", feedbackRepository.countByStatus(FEEDBACK_OPEN));
        data.put("todayOrderCount", ordersRepository.countByCreatedAtBetween(todayStart, tomorrowStart));
        data.put("finishedOrderCount", ordersRepository.countByStatus(ORDER_STATUS_FINISHED));
        data.put("productPublishTrend", normalizeTrend(productRepository.dashboardPublishTrend()));
        data.put("orderStatusStats", normalizeOrderStatusStats(ordersRepository.adminOrderStatusStats()));
        return ApiResp.ok(data);
    }

    private List<Map<String, Object>> normalizeTrend(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> {
            Map<String, Object> item = new HashMap<>();
            item.put("date", asString(row.get("label"), "-"));
            item.put("count", asInt(row.get("value")));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> normalizeOrderStatusStats(List<Map<String, Object>> rows) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("待支付", 0);
        counts.put("已支付", 0);
        counts.put("已完成", 0);
        counts.put("已取消", 0);

        for (Map<String, Object> row : rows) {
            String raw = asString(row.get("label"), "");
            int value = asInt(row.get("value"));
            if (ORDER_STATUS_UNPAID.equals(raw)) {
                counts.put("待支付", value);
            } else if (ORDER_STATUS_PAID.equals(raw)) {
                counts.put("已支付", value);
            } else if (ORDER_STATUS_FINISHED.equals(raw)) {
                counts.put("已完成", value);
            } else if (ORDER_STATUS_CANCELLED.equals(raw)) {
                counts.put("已取消", value);
            }
        }

        List<Map<String, Object>> stats = new ArrayList<>();
        counts.forEach((name, value) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            item.put("value", value);
            stats.add(item);
        });
        return stats;
    }

    private String asString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private int asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(0, RoundingMode.HALF_UP).intValue();
        }
        try {
            return new BigDecimal(String.valueOf(value)).setScale(0, RoundingMode.HALF_UP).intValue();
        } catch (Exception ignored) {
            return 0;
        }
    }
}
