package com.campus.twohand.user.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.credit.service.CreditService;
import com.campus.twohand.order.repo.OrdersRepository;
import com.campus.twohand.product.entity.Product;
import com.campus.twohand.product.entity.ProductImage;
import com.campus.twohand.product.repo.ProductImageRepository;
import com.campus.twohand.product.repo.ProductRepository;
import com.campus.twohand.product.support.ProductImageSanitizer;
import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.SysUserRepository;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 用户个人中心及公开资料服务
 * <p>
 * 负责处理与用户个人信息相关的业务逻辑，包括：
 * 1. 当前登录用户的个人信息管理（查看、修改、改密）。
 * 2. 其他用户的公开主页信息展示（基本资料、发布的商品、信誉评价）。
 */
@Service
public class UserProfileService {

    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private final SysUserRepository userRepo;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final OrdersRepository ordersRepository;
    private final CreditService creditService;
    private final PasswordEncoder passwordEncoder;
    private final SessionAuthSupport sessionAuthSupport;

    public UserProfileService(SysUserRepository userRepo,
                              ProductRepository productRepository,
                              ProductImageRepository productImageRepository,
                              OrdersRepository ordersRepository,
                              CreditService creditService,
                              PasswordEncoder passwordEncoder,
                              SessionAuthSupport sessionAuthSupport) {
        this.userRepo = userRepo;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.ordersRepository = ordersRepository;
        this.creditService = creditService;
        this.passwordEncoder = passwordEncoder;
        this.sessionAuthSupport = sessionAuthSupport;
    }

    /**
     * 获取当前登录用户的个人信息
     *
     * @param request HTTP请求，用于获取用户ID
     * @return 包含用户详细信息的响应
     */
    public ApiResp<?> me(HttpServletRequest request) {
        Long uid = sessionAuthSupport.requireUserId(request);
        SysUser user = userRepo.findById(uid)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", user.getId());
        resp.put("username", user.getUsername());
        resp.put("role", user.getRole());
        resp.put("name", user.getName());
        resp.put("phone", user.getPhone());
        resp.put("email", user.getEmail());
        resp.put("avatar", user.getAvatar());
        resp.put("isSuperAdmin", sessionAuthSupport.isSuperAdmin(user));
        return ApiResp.ok(resp);
    }

    /**
     * 更新当前登录用户的个人资料
     *
     * @param request HTTP请求，用于获取用户ID
     * @param name    新的昵称
     * @param phone   新的手机号
     * @param email   新的邮箱
     * @param avatar  新的头像URL
     * @return 操作结果
     */
    public ApiResp<?> updateMe(HttpServletRequest request, String name, String phone, String email, String avatar) {
        Long uid = sessionAuthSupport.requireUserId(request);
        SysUser user = userRepo.findById(uid)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        String nextPhone = validatePhone(phone);
        String nextEmail = validateEmail(email);
        ensurePhoneAvailable(nextPhone, uid);
        ensureEmailAvailable(nextEmail, uid);

        user.setName(trimToNull(name));
        user.setPhone(nextPhone);
        user.setEmail(nextEmail);
        user.setAvatar(trimToNull(avatar));
        userRepo.save(user);
        return ApiResp.ok(null);
    }

    /**
     * 修改当前登录用户的密码
     *
     * 修改密码后会使当前 session 失效，强制用户重新登录以确保安全。
     * 返回的响应 code=401，通知前端需要重新登录。
     *
     * @param request    HTTP请求，用于获取用户ID
     * @param oldPwd     原密码
     * @param newPwd     新密码
     * @param confirmPwd 确认新密码
     * @return 操作结果，code=401 表示需要重新登录
     */
    public ApiResp<?> changePassword(HttpServletRequest request, String oldPwd, String newPwd, String confirmPwd) {
        Long uid = sessionAuthSupport.requireUserId(request);
        if (oldPwd == null || oldPwd.trim().isEmpty()) {
            throw new RuntimeException("原密码不能为空");
        }
        if (newPwd == null || newPwd.length() < 6 || newPwd.length() > 16) {
            throw new RuntimeException("新密码长度需在 6 到 16 位之间");
        }
        if (!newPwd.equals(confirmPwd)) {
            throw new RuntimeException("两次输入密码不一致");
        }

        SysUser user = userRepo.findById(uid)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!passwordEncoder.matches(oldPwd, user.getPasswordHash())) {
            throw new RuntimeException("原密码错误");
        }

        // 更新新密码（使用 BCrypt 加密存储）
        user.setPasswordHash(passwordEncoder.encode(newPwd));
        userRepo.save(user);

        // ===== 安全措施：使当前 session 失效，强制重新登录 =====
        // 这样即使旧 session 被劫持，攻击者也无法继续使用
        HttpSession session = request.getSession(false);
        if (session != null) {
            // 使 session 失效，清除服务器端所有会话数据
            session.invalidate();
        }

        // 返回需要重新登录的响应，code=401，前端据此触发退出登录逻辑
        return ApiResp.reLogin("密码修改成功，请重新登录");
    }

    /**
     * 获取指定用户的公开主页信息
     *
     */
    public ApiResp<?> publicProfile(Long userId) {
        SysUser user = requirePublicUser(userId);

        Specification<Product> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("sellerId"), user.getId()),
                cb.equal(root.get("auditStatus"), "APPROVED")
        );

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", user.getId());
        resp.put("userId", user.getId());
        resp.put("nickname", resolveDisplayName(user));
        resp.put("name", user.getName());
        resp.put("avatar", user.getAvatar());
        resp.put("school", user.getSchool());
        resp.put("verifyStatus", user.getVerifyStatus());
        Integer creditScore = user.getCreditScore();
        resp.put("creditScore", creditScore == null ? 100 : creditScore);
        resp.put("publishedCount", productRepository.count(spec));
        return ApiResp.ok(resp);
    }

    /**
     * 获取指定用户发布的公开商品列表（分页）
     */
    public ApiResp<?> publicProducts(Long userId, int page, int size, String status) {
        SysUser user = requirePublicUser(userId);
        int currentPage = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        String statusFilter = normalizeStatusFilter(status);

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("sellerId"), user.getId()));
            predicates.add(cb.equal(root.get("auditStatus"), "APPROVED"));
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        List<Product> products = productRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, List<ProductImage>> imageMap = loadProductImageMap(productIds);

        List<Map<String, Object>> filteredRecords = new ArrayList<>();
        for (Product product : products) {
            boolean sold = ordersRepository.existsByProductIdAndStatusNot(product.getId(), "CANCELLED");
            String displayStatus = sold ? "SOLD" : ("ON".equals(product.getStatus()) ? "ON" : "OFF");
            if (statusFilter != null && !statusFilter.equals(displayStatus)) {
                continue;
            }

            List<ProductImage> images = imageMap.getOrDefault(product.getId(), Collections.emptyList());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", product.getId());
            item.put("title", product.getTitle());
            item.put("price", product.getPrice());
            item.put("status", product.getStatus());
            item.put("displayStatus", displayStatus);
            item.put("auditStatus", product.getAuditStatus());
            item.put("createdAt", product.getCreatedAt());
            item.put("viewCount", product.getViewCount());
            item.put("coverUrl", resolveCoverUrl(images));
            item.put("images", images);
            filteredRecords.add(item);
        }

        int total = filteredRecords.size();
        int start = Math.min((currentPage - 1) * pageSize, total);
        int end = Math.min(start + pageSize, total);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", filteredRecords.subList(start, end));
        data.put("total", total);
        data.put("page", currentPage);
        data.put("size", pageSize);
        return ApiResp.ok(data);
    }

    /**
     * 获取指定用户的公开信誉评价记录
     */
    public ApiResp<?> publicCredit(Long userId, int page, int size, String filter) {
        requirePublicUser(userId);
        Map<String, Object> data = new LinkedHashMap<>(creditService.sellerReviews(userId, page, size));
        return ApiResp.ok(data);
    }


    /**
     * 校验并返回一个可供公开访问的用户实体
     */
    private SysUser requirePublicUser(Long userId) {
        SysUser user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"USER".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    /**
     * 解析用户在公开场合的显示名称（优先用昵称，否则用通用名称）
     */
    private String resolveDisplayName(SysUser user) {
        String nickname = trimToNull(user.getName());
        if (nickname != null) {
            return nickname;
        }
        return "校园用户"; // Unicode for "校园用户"
    }

    /**
     * 标准化商品状态过滤器
     */
    private String normalizeStatusFilter(String status) {
        String value = trimToNull(status);
        if (value == null) {
            return null;
        }
        return switch (value.toUpperCase()) {
            case "ON", "OFF", "SOLD" -> value.toUpperCase();
            default -> null;
        };
    }

    /**
     * 批量加载并映射商品图片
     */
    private Map<Long, List<ProductImage>> loadProductImageMap(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ProductImage> images = productImageRepository.findByProductIdInOrderByProductIdAscSortAscIdAsc(productIds);
        Map<Long, List<ProductImage>> imageMap = new HashMap<>();
        for (ProductImage image : images) {
            String sanitized = ProductImageSanitizer.sanitize(image.getUrl());
            if (sanitized == null) {
                continue;
            }
            image.setUrl(sanitized);
            imageMap.computeIfAbsent(image.getProductId(), key -> new ArrayList<>()).add(image);
        }
        return imageMap;
    }

    /**
     * 解析商品封面图URL
     */
    private String resolveCoverUrl(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return ProductImageSanitizer.DEFAULT_PRODUCT_PLACEHOLDER;
        }
        return ProductImageSanitizer.coverOrPlaceholder(images.get(0).getUrl());
    }

    /**
     * 标准化公开订单的轻量信息
     */
    private List<Map<String, Object>> normalizePublicOrderLite(List<Map<String, Object>> rows, Set<Long> visibleOrderIds) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long orderId = parseLongValue(row.get("id"));
            if (orderId == null || (visibleOrderIds != null && !visibleOrderIds.isEmpty() && !visibleOrderIds.contains(orderId))) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", orderId);
            item.put("buyerId", row.get("buyerId"));
            item.put("buyerName", row.get("buyerName"));
            item.put("buyerAvatar", row.get("buyerAvatar"));
            item.put("sellerId", row.get("sellerId"));
            item.put("sellerName", row.get("sellerName"));
            item.put("sellerAvatar", row.get("sellerAvatar"));
            list.add(item);
        }
        return list;
    }

    /**
     * 为信誉记录填充评价来源信息
     */
    private void enrichPublicReviewSource(List<Map<String, Object>> records,
                                          List<Map<String, Object>> buyOrders,
                                          List<Map<String, Object>> sellOrders) {
        Map<Long, Map<String, Object>> buyOrderMap = toOrderMap(buyOrders);
        Map<Long, Map<String, Object>> sellOrderMap = toOrderMap(sellOrders);

        for (Map<String, Object> record : records) {
            Long orderId = parseLongValue(record.get("bizId"));
            if (orderId == null) {
                fillSystemReviewSource(record);
                continue;
            }

            Map<String, Object> sellOrder = sellOrderMap.get(orderId);
            if (sellOrder != null) {
                fillReviewSource(record, sellOrder.get("buyerId"), sellOrder.get("buyerName"),
                        sellOrder.get("buyerAvatar"), "来自买家");
                continue;
            }

            Map<String, Object> buyOrder = buyOrderMap.get(orderId);
            if (buyOrder != null) {
                fillReviewSource(record, buyOrder.get("sellerId"), buyOrder.get("sellerName"),
                        buyOrder.get("sellerAvatar"), "来自卖家");
                continue;
            }

            fillSystemReviewSource(record);
        }
    }

    /**
     * 将订单列表转换为以订单ID为key的Map
     */
    private Map<Long, Map<String, Object>> toOrderMap(List<Map<String, Object>> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Map<String, Object>> map = new HashMap<>();
        for (Map<String, Object> order : orders) {
            Long id = parseLongValue(order.get("id"));
            if (id != null) {
                map.put(id, order);
            }
        }
        return map;
    }

    /**
     * 填充评价来源信息
     */
    private void fillReviewSource(Map<String, Object> record,
                                  Object sourceUserId,
                                  Object displayName,
                                  Object avatar,
                                  String roleText) {
        String name = stringValue(displayName);
        record.put("sourceUserId", sourceUserId);
        record.put("sourceUserName", name);
        record.put("sourceNickName", name);
        record.put("sourceAvatar", stringValue(avatar));
        record.put("sourceRoleText", roleText);
    }

    /**
     * 填充系统来源的评价信息
     */
    private void fillSystemReviewSource(Map<String, Object> record) {
        record.put("sourceUserId", null);
        record.put("sourceUserName", "系统");
        record.put("sourceNickName", "系统");
        record.put("sourceAvatar", "");
        record.put("sourceRoleText", "系统");
    }

    /**
     * 过滤出可公开显示的信誉记录
     */
    private List<Map<String, Object>> filterPublicReviewRecords(Object recordsObj) {
        if (!(recordsObj instanceof List<?> rows) || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Object rowObj : rows) {
            if (!(rowObj instanceof Map<?, ?> rawRow)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            rawRow.forEach((key, value) -> row.put(String.valueOf(key), value));
            if (isPublicReviewRecord(row)) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    /**
     * 判断单条信誉记录是否可公开
     */
    private boolean isPublicReviewRecord(Map<String, Object> row) {
        String reason = stringValue(row.get("reason"));
        String bizType = stringValue(row.get("bizType"));
        String remark = stringValue(row.get("remark"));

        if (!"ORDER_FINISH".equalsIgnoreCase(reason) && !bizType.toLowerCase().contains("order")) {
            return false;
        }
        if ("INIT".equalsIgnoreCase(bizType) || "SYSTEM".equalsIgnoreCase(bizType)) {
            return false;
        }

        String combined = (reason + " " + bizType + " " + remark).toLowerCase();
        return !containsDirtyReviewKeyword(combined);
    }

    /**
     * 检查文本是否包含不应公开的内部测试关键词
     */
    private boolean containsDirtyReviewKeyword(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.contains("批量初始化")
                || text.contains("模拟订单")
                || text.contains("初始化")
                || text.contains("系统记录")
                || text.contains("系统生成")
                || text.contains("测试")
                || text.contains("mock");
    }

    /**
     * 从信誉记录中提取所有相关的订单ID
     */
    private Set<Long> extractVisibleOrderIds(List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> ids = new LinkedHashSet<>();
        for (Map<String, Object> row : records) {
            Long bizId = parseLongValue(row.get("bizId"));
            if (bizId != null && bizId > 0) {
                ids.add(bizId);
            }
        }
        return ids;
    }

    /**
     * 安全地将对象解析为Long
     */
    private Long parseLongValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 安全地将对象转为String
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 将字符串trim，如果为空则返回null
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 验证并返回合法的手机号
     */
    private String validatePhone(String value) {
        String phone = trimToNull(value);
        if (phone == null) {
            return null;
        }
        if (phone.length() > 20) {
            throw new RuntimeException("手机号长度不能超过 20");
        }
        if (!phone.matches(PHONE_PATTERN)) {
            /**
             * 手机号 = 1 + (3-9之一) + 9个数字
             *       = 1 + 1位 + 9位
             *       = 11位
             * */
            throw new RuntimeException("手机号格式不正确");
        }
        return phone;
    }

    /**
     * 验证并返回合法的邮箱地址
     */
    private String validateEmail(String value) {
        String email = trimToNull(value);
        if (email == null) {
            return null;
        }
        if (email.length() > 100) {
            throw new RuntimeException("邮箱长度不能超过 100");
        }
        if (!email.matches(EMAIL_PATTERN)) {
            throw new RuntimeException("邮箱格式不正确");
        }
        return email;
    }

    /**
     * 确保手机号未被其他用户占用
     */
    private void ensurePhoneAvailable(String phone, Long currentUserId) {
        if (phone == null) {
            return;
        }
        userRepo.findByPhone(phone)
                .filter(user -> !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new RuntimeException("手机号已存在");
                });
    }

    /**
     * 确保邮箱地址未被其他用户占用
     */
    private void ensureEmailAvailable(String email, Long currentUserId) {
        if (email == null) {
            return;
        }
        userRepo.findByEmail(email)
                .filter(user -> !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new RuntimeException("邮箱已存在");
                });
    }
}
