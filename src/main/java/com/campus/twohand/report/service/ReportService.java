package com.campus.twohand.report.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.product.entity.Product;
import com.campus.twohand.product.repo.ProductRepository;
import com.campus.twohand.report.entity.Report;
import com.campus.twohand.report.repo.ReportRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String STATUS_PENDING = "PENDING";

    private final SessionAuthSupport authSupport;
    private final ReportRepository reportRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ApiResp<?> submit(HttpServletRequest request, Map<String, Object> body) {
        Long reporterId = authSupport.requireUserId(request);
        Long productId = parseRequiredLong(body.get("productId"), "举报商品不能为空");
        String reason = normalizeRequiredText(body.get("reason"), "请选择举报原因", 100);
        String detail = normalizeOptionalText(body.get("detail"), 500);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("举报商品不存在"));

        if (Objects.equals(reporterId, product.getSellerId())) {
            throw new RuntimeException("不能举报自己发布的商品");
        }

        boolean existsPending = reportRepository.existsByReporterIdAndProductIdAndStatus(
                reporterId,
                productId,
                STATUS_PENDING
        );
        if (existsPending) {
            throw new RuntimeException("你已提交过待处理举报，请勿重复提交");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setProductId(productId);
        report.setReason(reason);
        report.setDetail(detail);
        report.setStatus(STATUS_PENDING);

        Report saved = reportRepository.save(report);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("status", saved.getStatus());
        return ApiResp.ok(result);
    }

    public ApiResp<?> myPage(HttpServletRequest request,
                             Long headerUserId,
                             int page,
                             int size,
                             String keyword,
                             String status) {
        Long currentUserId = authSupport.requireUserId(request);
        if (headerUserId != null && headerUserId > 0 && !Objects.equals(currentUserId, headerUserId)) {
            throw new RuntimeException("无权查看其他用户的举报记录");
        }

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedStatus = normalizeStatusFilter(status);
        int offset = (safePage - 1) * safeSize;

        long total = reportRepository.myCount(currentUserId, normalizedKeyword, normalizedStatus);
        List<Map<String, Object>> records = total > 0
                ? reportRepository.myPage(currentUserId, normalizedKeyword, normalizedStatus, safeSize, offset)
                : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return ApiResp.ok(result);
    }

    public ApiResp<?> adminPage(HttpServletRequest request,
                                int page,
                                int size,
                                String keyword,
                                String status) {
        authSupport.requireAdminId(request);

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedStatus = normalizeStatusFilter(status);
        int offset = (safePage - 1) * safeSize;

        long total = reportRepository.adminCount(normalizedKeyword, normalizedStatus);
        List<Map<String, Object>> records = total > 0
                ? reportRepository.adminPage(normalizedKeyword, normalizedStatus, safeSize, offset)
                : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return ApiResp.ok(result);
    }

    @Transactional
    public ApiResp<?> handle(HttpServletRequest request, Map<String, Object> body) {
        Long adminId = authSupport.requireAdminId(request);
        Long id = parseRequiredLong(body.get("id"), "举报记录不能为空");
        String status = normalizeHandleStatus(body.get("status"));
        String handleRemark = normalizeRequiredText(body.get("handleRemark"), "请输入处理说明", 500);

        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("举报记录不存在"));

        if (!STATUS_PENDING.equalsIgnoreCase(report.getStatus())) {
            throw new RuntimeException("该举报已处理，请勿重复操作");
        }

        String finalHandleRemark = handleRemark;
        if ("VALID".equals(status)) {
            finalHandleRemark = rejectReportedProductIfExists(report.getProductId(), handleRemark);
        }

        report.setStatus(status);
        report.setHandleRemark(finalHandleRemark);
        report.setHandledBy(adminId);
        report.setHandledAt(LocalDateTime.now());
        Report saved = reportRepository.save(report);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("status", saved.getStatus());
        result.put("handledAt", saved.getHandledAt());
        return ApiResp.ok(result);
    }

    private String normalizeStatusFilter(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String status = raw.trim().toUpperCase();
        if (!List.of("PENDING", "VALID", "INVALID", "HANDLED").contains(status)) {
            throw new RuntimeException("举报状态不支持");
        }
        return status;
    }

    private String rejectReportedProductIfExists(Long productId, String handleRemark) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return handleRemark + "；商品不存在，无法同步下架";
        }

        product.setStatus("OFF");
        product.setAuditStatus("REJECTED");
        product.setAuditReason(limitText("举报成立：" + handleRemark, 255));
        productRepository.save(product);
        return handleRemark;
    }

    private String normalizeHandleStatus(Object raw) {
        String status = String.valueOf(raw == null ? "" : raw).trim().toUpperCase();
        if (!List.of("VALID", "INVALID", "HANDLED").contains(status)) {
            throw new RuntimeException("处理状态不支持");
        }
        return status;
    }

    private Long parseRequiredLong(Object raw, String message) {
        if (raw == null) {
            throw new RuntimeException(message);
        }
        try {
            long value = Long.parseLong(String.valueOf(raw).trim());
            if (value <= 0) {
                throw new RuntimeException(message);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new RuntimeException(message);
        }
    }

    private String normalizeRequiredText(Object raw, String emptyMessage, int maxLength) {
        String value = String.valueOf(raw == null ? "" : raw).trim();
        if (value.isEmpty()) {
            throw new RuntimeException(emptyMessage);
        }
        if (value.length() > maxLength) {
            throw new RuntimeException("内容长度不能超过" + maxLength + "个字符");
        }
        return value;
    }

    private String normalizeOptionalText(Object raw, int maxLength) {
        String value = String.valueOf(raw == null ? "" : raw).trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > maxLength) {
            throw new RuntimeException("内容长度不能超过" + maxLength + "个字符");
        }
        return value;
    }

    private String normalizeKeyword(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
