package com.campus.twohand.product.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campus.twohand.chat.repo.ChatSessionRepository;
import com.campus.twohand.comment.repo.CommentRepository;
import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.order.repo.OrdersRepository;
import com.campus.twohand.product.entity.Product;
import com.campus.twohand.product.entity.ProductImage;
import com.campus.twohand.product.repo.FavoriteRepository;
import com.campus.twohand.product.repo.ProductImageRepository;
import com.campus.twohand.product.repo.ProductLikeRepository;
import com.campus.twohand.product.repo.ProductRepository;
import com.campus.twohand.product.support.ProductImageSanitizer;
import com.campus.twohand.report.repo.ReportRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final OrdersRepository ordersRepository;
    private final ReportRepository reportRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final FavoriteRepository favoriteRepository;
    private final ProductLikeRepository productLikeRepository;
    private final CommentRepository commentRepository;
    private final SessionAuthSupport sessionAuthSupport;

    public ApiResp<?> page(HttpServletRequest request,
            int page,
            int size,
            String keyword,
            String school,
            String auditStatus,
            String saleStatus) {
        sessionAuthSupport.requireAdminId(request);

        int limit = size;
        int offset = Math.max(page - 1, 0) * size;
        String kw = keyword == null ? null : keyword.trim();
        String schoolName = school == null ? null : school.trim();
        String audit = auditStatus == null ? null : auditStatus.trim();
        String sale = saleStatus == null ? null : saleStatus.trim();

        List<Map<String, Object>> raw = productRepository.adminPage(kw, schoolName, audit, sale, limit, offset);
        List<Map<String, Object>> list = new ArrayList<>();
        List<Long> ids = new ArrayList<>();

        for (Map<String, Object> row : raw) {
            Map<String, Object> item = new HashMap<>(row);
            list.add(item);

            Object idObj = item.get("id");
            if (idObj != null) {
                ids.add(((Number) idObj).longValue());
            }
        }

        if (!ids.isEmpty()) {
            List<ProductImage> images = productImageRepository.findByProductIdInOrderByProductIdAscSortAscIdAsc(ids);
            Map<Long, String> coverMap = new HashMap<>();
            for (ProductImage image : images) {
                String sanitized = ProductImageSanitizer.sanitize(image.getUrl());
                if (sanitized != null) {
                    coverMap.putIfAbsent(image.getProductId(), sanitized);
                }
            }
            for (Map<String, Object> item : list) {
                Long productId = ((Number) item.get("id")).longValue();
                item.put("coverUrl", coverMap.getOrDefault(productId, ProductImageSanitizer.DEFAULT_PRODUCT_PLACEHOLDER));
            }
        }

        long total = productRepository.adminCount(kw, schoolName, audit, sale);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        return ApiResp.ok(data);
    }

    public ApiResp<?> detail(HttpServletRequest request, Long id) {
        sessionAuthSupport.requireAdminId(request);

        Map<String, Object> raw = productRepository.adminDetail(id);
        if (raw == null) {
            return ApiResp.fail("商品不存在");
        }

        Map<String, Object> data = new HashMap<>(raw);
        List<ProductImage> images = new ArrayList<>();
        for (ProductImage image : productImageRepository.findByProductIdOrderBySortAscIdAsc(id)) {
            String sanitized = ProductImageSanitizer.sanitize(image.getUrl());
            if (sanitized == null) {
                continue;
            }
            image.setUrl(sanitized);
            images.add(image);
        }
        data.put("images", images);
        data.put("coverUrl", images.isEmpty()
                ? ProductImageSanitizer.DEFAULT_PRODUCT_PLACEHOLDER
                : ProductImageSanitizer.coverOrPlaceholder(images.get(0).getUrl()));
        return ApiResp.ok(data);
    }

    public ApiResp<?> approve(HttpServletRequest request, Long id) {
        sessionAuthSupport.requireAdminId(request);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        rejectIfSold(product);
        product.setAuditStatus("APPROVED");
        product.setAuditReason(null);
        product.setStatus("ON");
        productRepository.save(product);
        return ApiResp.ok(null);
    }

    public ApiResp<?> reject(HttpServletRequest request, Long id, String reason) {
        sessionAuthSupport.requireAdminId(request);

        String rejectReason = reason == null ? "" : reason.trim();
        if (rejectReason.isEmpty()) {
            throw new RuntimeException("请输入驳回原因");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        rejectIfSold(product);
        product.setAuditStatus("REJECTED");
        product.setAuditReason(rejectReason);
        product.setStatus("OFF");
        productRepository.save(product);
        return ApiResp.ok(null);
    }

    public ApiResp<?> off(HttpServletRequest request, Long id, String reason) {
        sessionAuthSupport.requireAdminId(request);

        String offReason = reason == null ? "" : reason.trim();
        if (offReason.isEmpty()) {
            throw new RuntimeException("请输入下架原因");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        rejectIfSold(product);
        product.setStatus("OFF");
        product.setAuditStatus("APPROVED");
        product.setAuditReason(formatAdminOffReason(offReason));
        productRepository.save(product);
        return ApiResp.ok(null);
    }

    public ApiResp<?> on(HttpServletRequest request, Long id) {
        sessionAuthSupport.requireAdminId(request);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        rejectIfSold(product);
        if (!"APPROVED".equals(product.getAuditStatus())) {
            throw new RuntimeException("仅审核通过的商品允许重新上架");
        }
        product.setStatus("ON");
        productRepository.save(product);
        return ApiResp.ok(null);
    }

    @Transactional
    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        if (!sessionAuthSupport.isSuperAdmin(sessionAuthSupport.requireAdmin(request))) {
            return ApiResp.fail("仅超级管理员可删除商品");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        ensureProductDeletable(product);
        productImageRepository.deleteByProductId(id);
        productRepository.deleteById(id);
        return ApiResp.ok(null);
    }

    @Transactional
    public ApiResp<?> batchDelete(HttpServletRequest request, List<Long> ids) {
        if (!sessionAuthSupport.isSuperAdmin(sessionAuthSupport.requireAdmin(request))) {
            return ApiResp.fail("仅超级管理员可删除商品");
        }
        if (ids == null || ids.isEmpty()) {
            return ApiResp.ok(null);
        }

        List<Product> products = productRepository.findAllById(ids);
        if (products.size() != ids.size()) {
            throw new RuntimeException("商品不存在");
        }
        for (Product product : products) {
            ensureProductDeletable(product);
        }
        for (Long id : ids) {
            productImageRepository.deleteByProductId(id);
        }
        productRepository.deleteAllById(ids);
        return ApiResp.ok(null);
    }

    private String formatAdminOffReason(String reason) {
        String safeReason = reason == null ? "" : reason.trim();
        String result = "管理员下架：" + safeReason;

        // audit_reason 字段长度一般为 255，这里避免保存内容过长
        if (result.length() > 255) {
            return result.substring(0, 255);
        }

        return result;
    }

    private void rejectIfSold(Product product) {
        if (product.getSoldFlag() != null && product.getSoldFlag() == 1) {
            throw new RuntimeException("已售商品不能执行该操作");
        }
    }

    private void ensureProductDeletable(Product product) {
        rejectIfSold(product);

        Long productId = product.getId();
        boolean hasBusinessRecords = ordersRepository.existsByProductId(productId)
                || reportRepository.existsByProductId(productId)
                || chatSessionRepository.existsByProductId(productId)
                || favoriteRepository.countByProductIdAndStatus(productId, 1) > 0
                || productLikeRepository.countByProductIdAndStatus(productId, 1) > 0
                || commentRepository.countByTargetTypeAndTargetIdAndStatus("PRODUCT", productId, 1) > 0;

        if (hasBusinessRecords) {
            throw new RuntimeException("该商品已有业务记录，不能删除，可进行下架处理");
        }
    }
}
