package com.campus.twohand.order.service;

import com.campus.twohand.address.entity.UserAddress;
import com.campus.twohand.address.repo.UserAddressRepository;
import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.credit.service.CreditService;
import com.campus.twohand.order.entity.Orders;
import com.campus.twohand.order.repo.OrdersRepository;
import com.campus.twohand.product.entity.Product;
import com.campus.twohand.product.repo.ProductRepository;
import com.campus.twohand.product.support.ProductImageSanitizer;
import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.SysUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private static final long UNPAID_TIMEOUT_MINUTES = 30L;
    private static final long PAID_AUTO_FINISH_DAYS = 7L;

    private final OrdersRepository ordersRepository;
    private final ProductRepository productRepository;
    private final UserAddressRepository userAddressRepository;
    private final CreditService creditService;
    private final SessionAuthSupport sessionAuthSupport;
    private final SysUserRepository sysUserRepository;

    public OrderService(OrdersRepository ordersRepository,
                        ProductRepository productRepository,
                        UserAddressRepository userAddressRepository,
                        CreditService creditService,
                        SessionAuthSupport sessionAuthSupport,
                        SysUserRepository sysUserRepository) {
        this.ordersRepository = ordersRepository;
        this.productRepository = productRepository;
        this.userAddressRepository = userAddressRepository;
        this.creditService = creditService;
        this.sessionAuthSupport = sessionAuthSupport;
        this.sysUserRepository = sysUserRepository;
    }

    @Transactional
    public ApiResp<?> myPage(HttpServletRequest request,
                             String type,
                             int page,
                             int size,
                             String keyword,
                             String status,
                             String payType) {
        Long uid = sessionAuthSupport.requireUserId(request);
        LocalDateTime now = LocalDateTime.now();
        if (!"SELL".equalsIgnoreCase(type)) {
            autoFinishExpiredPaidOrders(uid, now);
        }
        autoCancelExpiredUnpaidOrders(uid, type, now);

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1));
        Page<Map<String, Object>> paged = "SELL".equalsIgnoreCase(type)
                ? ordersRepository.userSellerPage(uid, keyword, status, payType, pageable)
                : ordersRepository.userBuyerPage(uid, keyword, status, payType, pageable);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : paged.getContent()) {
            Map<String, Object> item = new HashMap<>(row);
            item.put("coverUrl", ProductImageSanitizer.coverOrPlaceholder(row.get("coverUrl")));
            records.add(item);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("total", paged.getTotalElements());
        resp.put("records", records);
        return ApiResp.ok(resp);
    }

    @Transactional
    public ApiResp<?> create(HttpServletRequest request, Long productId, Long addressId) {
        Long uid = sessionAuthSupport.requireUserId(request);
        Long orderId = createOrder(uid, productId, addressId);
        return ApiResp.ok(orderId);
    }

    @Transactional
    public ApiResp<?> pay(HttpServletRequest request, Long id, String payType) {
        Long uid = sessionAuthSupport.requireUserId(request);
        Orders order = ordersRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("order not found"));

        if (!uid.equals(order.getBuyerId())) {
            throw new RuntimeException("no permission");
        }

        LocalDateTime now = LocalDateTime.now();
        if (cancelExpiredOrderIfNeeded(order, now)) {
            throw new RuntimeException("\u8ba2\u5355\u5df2\u8d85\u65f6\u81ea\u52a8\u53d6\u6d88");
        }
        if (!"UNPAID".equals(order.getStatus())) {
            throw new RuntimeException("order status does not allow pay");
        }

        String normalizedPayType = normalizePayType(payType);
        int updated = ordersRepository.updatePaidInfo(id, normalizedPayType);
        if (updated == 0) {
            Orders latest = ordersRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("order not found"));
            if (isExpiredUnpaidOrder(latest, now) || isTimedOutCancelledOrder(latest, now)) {
                throw new RuntimeException("\u8ba2\u5355\u5df2\u8d85\u65f6\u81ea\u52a8\u53d6\u6d88");
            }
            throw new RuntimeException("order status does not allow pay");
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", id);
        resp.put("orderNo", order.getOrderNo());
        resp.put("status", "PAID");
        resp.put("payType", normalizedPayType);
        resp.put("payTypeText", "WECHAT".equals(normalizedPayType) ? "WECHAT" : "ALIPAY");
        return ApiResp.ok(resp);
    }

    @Transactional
    public ApiResp<?> cancel(HttpServletRequest request, Long id) {
        Long uid = sessionAuthSupport.requireUserId(request);
        Orders order = ordersRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("order not found"));

        if (!uid.equals(order.getBuyerId())) {
            throw new RuntimeException("no permission");
        }

        LocalDateTime now = LocalDateTime.now();
        if (cancelExpiredOrderIfNeeded(order, now)) {
            return ApiResp.ok(null);
        }
        cancelUnpaidOrder(order, now);
        return ApiResp.ok(null);
    }

    @Transactional
    public ApiResp<?> finish(HttpServletRequest request, Long id, String review) {
        Long uid = sessionAuthSupport.requireUserId(request);
        Orders order = ordersRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("order not found"));

        if (!uid.equals(order.getBuyerId())) {
            throw new RuntimeException("no permission");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new RuntimeException("order status does not allow finish");
        }
        LocalDateTime now = LocalDateTime.now();

        String normalizedReview = normalizeReview(review);
        finishPaidOrder(order, now, parseBuyerReview(normalizedReview, now));

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", id);
        resp.put("orderNo", order.getOrderNo());
        resp.put("status", "FINISHED");
        resp.put("reviewSubmitted", !normalizedReview.isEmpty());
        return ApiResp.ok(resp);
    }

    @Transactional
    public ApiResp<?> hide(HttpServletRequest request, Long id) {
        Long uid = sessionAuthSupport.requireUserId(request);
        Orders order = ordersRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("order not found"));

        if (!"FINISHED".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("only FINISHED/CANCELLED can be hidden");
        }

        LocalDateTime now = LocalDateTime.now();
        if (uid.equals(order.getBuyerId())) {
            order.setBuyerVisible(Boolean.FALSE);
        } else if (uid.equals(order.getSellerId())) {
            order.setSellerVisible(Boolean.FALSE);
        } else {
            throw new RuntimeException("no permission");
        }

        order.setUpdatedAt(now);
        ordersRepository.save(order);
        return ApiResp.ok(null);
    }

    @Transactional
    public Long createOrder(Long uid, Long productId, Long addressId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new RuntimeException("\u5546\u54c1\u4e0d\u5b58\u5728"));
        SysUser buyer = sysUserRepository.findById(uid)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String buyerSchool = verifiedSchoolOf(buyer);
        String productSchool = trimToNull(product.getSchoolName());
        if (productSchool == null || !buyerSchool.equals(productSchool)) {
            throw new RuntimeException("该商品不属于当前学校，无法购买");
        }

        if (!"APPROVED".equals(product.getAuditStatus())) {
            throw new RuntimeException("\u5f53\u524d\u5546\u54c1\u6682\u4e0d\u53ef\u8d2d\u4e70");
        }
        if (!"ON".equals(product.getStatus())) {
            throw new RuntimeException("\u5546\u54c1\u5df2\u4e0b\u67b6");
        }
        if (isSold(product)) {
            throw new RuntimeException("\u5546\u54c1\u5df2\u552e\u51fa");
        }
        if (uid.equals(product.getSellerId())) {
            throw new RuntimeException("\u4e0d\u80fd\u8d2d\u4e70\u81ea\u5df1\u53d1\u5e03\u7684\u5546\u54c1");
        }
        SysUser seller = sysUserRepository.findById(product.getSellerId())
                .orElseThrow(() -> new RuntimeException("卖家不存在"));
        if (!"VERIFIED".equalsIgnoreCase(seller.getVerifyStatus())) {
            throw new RuntimeException("卖家认证状态已变更，该商品暂不可交易");
        }

        if (ordersRepository.existsByProductIdAndStatusNot(productId, "CANCELLED")) {
            if (!"OFF".equals(product.getStatus())) {
                product.setStatus("OFF");
                productRepository.save(product);
            }
            throw new RuntimeException("\u5546\u54c1\u5df2\u88ab\u5176\u4ed6\u7528\u6237\u4e0b\u5355");
        }

        LocalDateTime now = LocalDateTime.now();
        Orders order = new Orders();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setProductId(product.getId());
        order.setProductTitle(product.getTitle());
        order.setProductPrice(product.getPrice());
        order.setAmount(product.getPrice());
        order.setBuyerId(uid);
        order.setSellerId(product.getSellerId());
        order.setStatus("UNPAID");
        order.setTradeLocation(normalizeTradeLocation(product.getAddressText()));
        
        // 处理地址
        if (addressId != null) {
            UserAddress address = userAddressRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("\u6536\u8d27\u5730\u5740\u4e0d\u5b58\u5728"));
            if (!uid.equals(address.getUserId()) || address.getStatus() == null || address.getStatus() != 1) {
                throw new RuntimeException("\u6536\u8d27\u5730\u5740\u4e0d\u53ef\u7528");
            }
            order.setAddressId(addressId);
            order.setContactName(address.getContactName());
            order.setContactPhone(address.getContactPhone());
            order.setAddressText(address.getAddressText());
        }
        
        order.setBuyerVisible(Boolean.TRUE);
        order.setSellerVisible(Boolean.TRUE);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        Orders saved = ordersRepository.save(order);

        product.setStatus("OFF");
        product.setSoldFlag(0);
        productRepository.save(product);

        return saved.getId();
    }

    public void restoreProductIfNeeded(Long productId) {
        productRepository.findById(productId).ifPresent(product -> {
            boolean hasNonCancelledOrder = ordersRepository.existsByProductIdAndStatusNot(productId, "CANCELLED");
            if (hasNonCancelledOrder) {
                if (!"OFF".equals(product.getStatus())) {
                    product.setStatus("OFF");
                    product.setSoldFlag(isSold(product) ? 1 : 0);
                    productRepository.save(product);
                }
                return;
            }
            product.setSoldFlag(0);
            if ("APPROVED".equals(product.getAuditStatus()) && !"ON".equals(product.getStatus())) {
                product.setStatus("ON");
            }
            productRepository.save(product);
        });
    }

    @Transactional
    public void cancelUnpaidOrderByAdmin(Orders order) {
        cancelUnpaidOrder(order, LocalDateTime.now());
    }

    @Transactional
    public int cancelAllExpiredUnpaidOrders(LocalDateTime now) {
        List<Orders> expiredOrders = ordersRepository.findByStatusAndCreatedAtLessThanEqual("UNPAID", unpaidDeadline(now));
        int count = 0;
        for (Orders order : expiredOrders) {
            if (cancelExpiredOrderIfNeeded(order, now)) {
                count++;
            }
        }
        return count;
    }

    @Transactional
    public int autoFinishAllExpiredPaidOrders(LocalDateTime now) {
        List<Orders> expiredOrders = ordersRepository
                .findByStatusAndPaidAtIsNotNullAndPaidAtLessThanEqualAndFinishedAtIsNull("PAID", paidAutoFinishDeadline(now));
        int count = 0;
        for (Orders order : expiredOrders) {
            if (finishExpiredPaidOrderIfNeeded(order, now)) {
                count++;
            }
        }
        return count;
    }

    private void autoCancelExpiredUnpaidOrders(Long uid, String type, LocalDateTime now) {
        LocalDateTime deadline = unpaidDeadline(now);
        List<Orders> expiredOrders = "SELL".equalsIgnoreCase(type)
                ? ordersRepository.findBySellerIdAndStatusAndCreatedAtLessThanEqual(uid, "UNPAID", deadline)
                : ordersRepository.findByBuyerIdAndStatusAndCreatedAtLessThanEqual(uid, "UNPAID", deadline);
        for (Orders order : expiredOrders) {
            cancelExpiredOrderIfNeeded(order, now);
        }
    }

    private void autoFinishExpiredPaidOrders(Long uid, LocalDateTime now) {
        List<Orders> expiredOrders = ordersRepository
                .findByBuyerIdAndStatusAndPaidAtIsNotNullAndPaidAtLessThanEqualAndFinishedAtIsNull(uid, "PAID", paidAutoFinishDeadline(now));
        for (Orders order : expiredOrders) {
            finishExpiredPaidOrderIfNeeded(order, now);
        }
    }

    private boolean cancelExpiredOrderIfNeeded(Orders order, LocalDateTime now) {
        if (!isExpiredUnpaidOrder(order, now)) {
            return false;
        }
        cancelUnpaidOrder(order, now);
        return true;
    }

    private void cancelUnpaidOrder(Orders order, LocalDateTime now) {
        if (!"UNPAID".equals(order.getStatus())) {
            throw new RuntimeException("order status does not allow cancel");
        }
        order.setStatus("CANCELLED");
        order.setCancelledAt(now);
        order.setUpdatedAt(now);
        ordersRepository.save(order);
        ordersRepository.flush();
        restoreProductIfNeeded(order.getProductId());
    }

    private boolean finishExpiredPaidOrderIfNeeded(Orders order, LocalDateTime now) {
        if (!isExpiredPaidOrder(order, now)) {
            return false;
        }
        finishPaidOrder(order, now, autoFinishReview());
        return true;
    }

    private void finishPaidOrder(Orders order, LocalDateTime now, BuyerReview review) {
        if (!"PAID".equals(order.getStatus()) || order.getFinishedAt() != null) {
            return;
        }
        order.setStatus("FINISHED");
        order.setFinishedAt(now);
        order.setUpdatedAt(now);
        if (review != null) {
            order.setBuyerRate(review.rate());
            order.setBuyerComment(review.comment());
            order.setReviewedAt(review.reviewedAt());
        }
        ordersRepository.save(order);
        productRepository.findById(order.getProductId()).ifPresent(product -> {
            product.setStatus("OFF");
            product.setSoldFlag(1);
            productRepository.save(product);
        });
        rewardOrderFinishOnce(order);
    }

    private void rewardOrderFinishOnce(Orders order) {
        creditService.rewardOrderFinishedOnce(order.getBuyerId(), order.getId(), 1);
        creditService.rewardOrderFinishedOnce(order.getSellerId(), order.getId(), 2);
    }

    private boolean isSold(Product product) {
        return product != null && Integer.valueOf(1).equals(product.getSoldFlag());
    }

    private boolean isExpiredUnpaidOrder(Orders order, LocalDateTime now) {
        return "UNPAID".equals(order.getStatus())
                && order.getCreatedAt() != null
                && !order.getCreatedAt().isAfter(unpaidDeadline(now));
    }

    private boolean isTimedOutCancelledOrder(Orders order, LocalDateTime now) {
        return "CANCELLED".equals(order.getStatus())
                && order.getPaidAt() == null
                && order.getCreatedAt() != null
                && !order.getCreatedAt().isAfter(unpaidDeadline(now));
    }

    private boolean isExpiredPaidOrder(Orders order, LocalDateTime now) {
        return "PAID".equals(order.getStatus())
                && order.getFinishedAt() == null
                && order.getPaidAt() != null
                && !order.getPaidAt().isAfter(paidAutoFinishDeadline(now));
    }

    private LocalDateTime unpaidDeadline(LocalDateTime now) {
        return now.minusMinutes(UNPAID_TIMEOUT_MINUTES);
    }

    private LocalDateTime paidAutoFinishDeadline(LocalDateTime now) {
        return now.minusDays(PAID_AUTO_FINISH_DAYS);
    }

    private BuyerReview parseBuyerReview(String normalizedReview, LocalDateTime reviewedAt) {
        if (normalizedReview == null || normalizedReview.isBlank()) {
            return new BuyerReview("GOOD", null, null);
        }

        String text = normalizedReview.trim();
        String rate = "GOOD";
        String comment = text;
        if (text.matches("^\\s*(好评|中评|差评)[:：]?.*")) {
            String label = text.replaceFirst("^\\s*(好评|中评|差评).*", "$1");
            rate = switch (label) {
                case "中评" -> "NEUTRAL";
                case "差评" -> "BAD";
                default -> "GOOD";
            };
            comment = text.replaceFirst("^\\s*(好评|中评|差评)[:：]?\\s*", "").trim();
        }

        return new BuyerReview(rate, comment.isEmpty() ? null : comment, reviewedAt);
    }

    private BuyerReview autoFinishReview() {
        return new BuyerReview("GOOD", null, null);
    }

    private record BuyerReview(String rate, String comment, LocalDateTime reviewedAt) {
    }

    private String normalizePayType(String payType) {
        String normalized = payType == null ? "WECHAT" : payType.trim().toUpperCase();
        if (!"WECHAT".equals(normalized) && !"ALIPAY".equals(normalized)) {
            throw new RuntimeException("only WECHAT or ALIPAY is supported");
        }
        return normalized;
    }

    private String normalizeReview(String review) {
        if (review == null) {
            return "";
        }
        String normalized = review.trim();
        if (normalized.length() > 120) {
            throw new RuntimeException("review is too long");
        }
        return normalized;
    }

    private String normalizeTradeLocation(String tradeLocation) {
        if (tradeLocation == null) {
            return null;
        }
        String normalized = tradeLocation.trim()
                .replaceFirst("^历史地址[:：]\\s*", "")
                .trim();
        if (normalized.matches(".*1[3-9]\\d{9}.*")) {
            return normalized.matches(".*(宿舍|楼栋|寝室).*") ? "宿舍区楼下" : "校内当面交易";
        }
        long separatorCount = normalized.chars().filter(ch -> ch == '|' || ch == '｜').count();
        if (separatorCount >= 2) {
            return normalized.matches(".*(宿舍|楼栋|寝室).*") ? "宿舍区楼下" : "校内当面交易";
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private String verifiedSchoolOf(SysUser user) {
        String school = trimToNull(user == null ? null : user.getSchool());
        if (user == null || !"VERIFIED".equalsIgnoreCase(user.getVerifyStatus()) || school == null) {
            throw new RuntimeException("请先完成学号认证");
        }
        return school;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}




