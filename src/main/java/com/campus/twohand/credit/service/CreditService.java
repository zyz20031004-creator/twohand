package com.campus.twohand.credit.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.credit.entity.CreditLog;
import com.campus.twohand.credit.repo.CreditLogRepository;
import com.campus.twohand.order.repo.OrdersRepository;
import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.SysUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreditService {

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 200;
    private static final String VERIFY_APPROVED_REASON = "VERIFY_APPROVED";
    private static final String ORDER_FINISHED_REASON = "ORDER_FINISHED";
    private static final String ORDER_BIZ_TYPE = "orders";

    private final CreditLogRepository creditLogRepository;
    private final OrdersRepository ordersRepository;
    private final SysUserRepository sysUserRepository;
    private final SessionAuthSupport sessionAuthSupport;

    public ApiResp<?> my(HttpServletRequest request, int page, int size) {
        Long userId = sessionAuthSupport.requireUserId(request);
        return ApiResp.ok(my(userId, page, size));
    }

    public ApiResp<?> myReviews(HttpServletRequest request, int page, int size) {
        Long userId = sessionAuthSupport.requireUserId(request);
        return ApiResp.ok(sellerReviews(userId, page, size));
    }

    public ApiResp<?> adminPage(HttpServletRequest request,
                                int page,
                                int size,
                                String keyword,
                                String verifyStatus,
                                Integer minScore,
                                Integer maxScore) {
        sessionAuthSupport.requireAdminId(request);

        if (minScore != null && maxScore != null && minScore > maxScore) {
            return ApiResp.fail("最低分不能大于最高分");
        }

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;
        String normalizedVerifyStatus = normalizeVerifyStatus(verifyStatus);

        long total = sysUserRepository.creditUserCount(keyword, normalizedVerifyStatus, minScore, maxScore);
        List<Map<String, Object>> records = sysUserRepository.creditUserPage(
                keyword,
                normalizedVerifyStatus,
                minScore,
                maxScore,
                safeSize,
                offset
        );

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("records", records);
        return ApiResp.ok(data);
    }

    public ApiResp<?> adminLogs(HttpServletRequest request, Long userId, int page, int size) {
        sessionAuthSupport.requireAdminId(request);
        if (userId == null) {
            return ApiResp.fail("userId不能为空");
        }

        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null || !"USER".equalsIgnoreCase(user.getRole())) {
            return ApiResp.fail("用户不存在");
        }

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        Page<CreditLog> result = creditLogRepository.findByUserIdOrderByIdDesc(userId, pageable);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        data.put("score", user.getCreditScore() == null ? 100 : user.getCreditScore());
        data.put("total", result.getTotalElements());
        data.put("records", result.getContent().stream().map(this::toCreditLogRow).toList());
        return ApiResp.ok(data);
    }

    @Transactional
    public ApiResp<?> adjust(HttpServletRequest request, Map<String, Object> body) {
        sessionAuthSupport.requireAdminId(request);

        Long userId = parseLong(body.get("userId"));
        Integer delta = parseInteger(body.get("delta"));
        String remark = trim(body.get("remark"));

        if (userId == null) {
            return ApiResp.fail("userId不能为空");
        }
        if (delta == null) {
            return ApiResp.fail("delta不能为空");
        }
        if (delta == 0) {
            return ApiResp.fail("调整分值不能为0");
        }
        if (remark.isEmpty()) {
            return ApiResp.fail("调整原因不能为空");
        }
        if (remark.length() < 2 || remark.length() > 64) {
            return ApiResp.fail("调整原因长度需在 2 到 64 个字符之间");
        }

        change(userId, delta, "ADMIN_ADJUST", "admin_credit", userId, remark);
        return ApiResp.ok("ok");
    }

    @Transactional
    public boolean rewardVerifyApprovedOnce(Long userId) {
        if (creditLogRepository.existsByUserIdAndReason(userId, VERIFY_APPROVED_REASON)) {
            return false;
        }
        change(userId, 5, VERIFY_APPROVED_REASON, "student_verify", null, "校园认证通过");
        return true;
    }

    @Transactional
    public boolean rewardOrderFinishedOnce(Long userId, Long orderId, Integer delta) {
        if (creditLogRepository.existsOrderFinishedReward(userId, ORDER_BIZ_TYPE, orderId)) {
            return false;
        }
        change(userId, delta, ORDER_FINISHED_REASON, ORDER_BIZ_TYPE, orderId, "交易完成加分");
        return true;
    }

    @Transactional
    public boolean changeOnce(Long userId,
                              Integer delta,
                              String reason,
                              String bizType,
                              Long bizId,
                              String remark) {
        if (creditLogRepository.existsByUserIdAndReasonAndBizTypeAndBizId(userId, reason, bizType, bizId)) {
            return false;
        }
        change(userId, delta, reason, bizType, bizId, remark);
        return true;
    }

    @Transactional
    public void change(Long userId,
                       Integer delta,
                       String reason,
                       String bizType,
                       Long bizId,
                       String remark) {
        if (userId == null) {
            throw new RuntimeException("userId娑撳秷鍏樻稉铏光敄");
        }
        int d = delta == null ? 0 : delta;

        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int oldScore = user.getCreditScore() == null ? 100 : user.getCreditScore();
        int nextScore = oldScore + d;
        if (nextScore < MIN_SCORE) {
            nextScore = MIN_SCORE;
        }
        if (nextScore > MAX_SCORE) {
            nextScore = MAX_SCORE;
        }

        user.setCreditScore(nextScore);
        sysUserRepository.save(user);

        CreditLog log = new CreditLog();
        log.setUserId(userId);
        log.setChangeVal(d);
        log.setReason(reason);
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        creditLogRepository.save(log);
    }

    public Map<String, Object> my(Long userId, int page, int size) {
        return my(userId, page, size, null);
    }

    public Map<String, Object> sellerReviews(Long userId, int page, int size) {
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("鐢ㄦ埛涓嶅瓨鍦?");
        }

        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        Pageable pageable = PageRequest.of(p - 1, s);
        Page<Map<String, Object>> result = ordersRepository.sellerFinishedReviewPage(userId, pageable);
        List<Map<String, Object>> records = result.getContent().stream().map(this::toSellerReviewRow).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("score", user.getCreditScore() == null ? 100 : user.getCreditScore());
        data.put("records", records);
        data.put("total", result.getTotalElements());
        return data;
    }

    public Map<String, Object> my(Long userId, int page, int size, String filter) {
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        Pageable pageable = PageRequest.of(p - 1, s);

        Page<CreditLog> result = queryCreditLogs(userId, filter, pageable);
        List<Map<String, Object>> records = result.getContent().stream().map(this::toCreditLogRow).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("score", user.getCreditScore() == null ? 100 : user.getCreditScore());
        data.put("records", records);
        data.put("total", result.getTotalElements());
        return data;
    }

    private Page<CreditLog> queryCreditLogs(Long userId, String filter, Pageable pageable) {
        String normalized = trim(filter).toUpperCase();
        if ("POSITIVE".equals(normalized)) {
            return creditLogRepository.findByUserIdAndChangeValGreaterThanOrderByIdDesc(userId, 0, pageable);
        }
        if ("NEGATIVE".equals(normalized)) {
            return creditLogRepository.findByUserIdAndChangeValLessThanOrderByIdDesc(userId, 0, pageable);
        }
        return creditLogRepository.findByUserIdOrderByIdDesc(userId, pageable);
    }

    private Map<String, Object> toCreditLogRow(CreditLog item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", item.getId());
        row.put("userId", item.getUserId());
        row.put("changeVal", item.getChangeVal());
        row.put("reason", normalizeReason(item.getReason()));
        row.put("bizType", item.getBizType());
        row.put("bizId", item.getBizId());
        row.put("remark", normalizeRemark(item.getReason(), item.getRemark()));
        row.put("createdAt", item.getCreatedAt());
        row.put("operatorName", null);
        return row;
    }

    private Map<String, Object> toSellerReviewRow(Map<String, Object> item) {
        Map<String, Object> row = new LinkedHashMap<>();
        String buyerRate = trim(item.get("buyerRate"));
        String buyerComment = trim(item.get("buyerComment"));
        String remark = trim(item.get("remark"));
        String content = !buyerComment.isEmpty() ? buyerComment : extractBuyerReviewContent(remark);
        String ratingLevel = resolveRatingLevel(!buyerRate.isEmpty() ? buyerRate : remark);

        row.put("id", item.get("id"));
        row.put("userId", item.get("userId"));
        row.put("changeVal", item.get("changeVal"));
        row.put("reason", normalizeReason(trim(item.get("reason"))));
        row.put("bizType", item.get("bizType"));
        row.put("bizId", item.get("bizId"));
        row.put("remark", content);
        row.put("reviewContent", content.isEmpty() ? "买家已完成交易" : content);
        row.put("ratingLevel", ratingLevel);
        row.put("ratingText", ratingText(ratingLevel));
        row.put("createdAt", item.get("reviewedAt") == null ? item.get("createdAt") : item.get("reviewedAt"));
        row.put("sourceUserId", item.get("sourceUserId"));
        row.put("sourceUserName", item.get("sourceUserName"));
        row.put("sourceNickName", item.get("sourceNickName"));
        row.put("sourceAvatar", item.get("sourceAvatar"));
        return row;
    }

    private String extractBuyerReviewContent(String remark) {
        String text = trim(remark);
        if (text.isEmpty()
                || "交易完成加分".equals(text)
                || "浜ゆ槗瀹屾垚鍔犲垎".equals(text)
                || "买家确认收货，交易完成".equals(text)) {
            return "";
        }

        String prefix = "买家确认收货并评价：";
        if (text.startsWith(prefix)) {
            text = text.substring(prefix.length()).trim();
        }
        if (text.matches("^(好评|中评|差评)[:：]\\s*.*")) {
            return text.replaceFirst("^(好评|中评|差评)[:：]\\s*", "").trim();
        }
        return text;
    }

    private String resolveRatingLevel(String remark) {
        String text = trim(remark).toLowerCase();
        if ("bad".equals(text)) {
            return "BAD";
        }
        if ("neutral".equals(text)) {
            return "NEUTRAL";
        }
        if ("good".equals(text)) {
            return "GOOD";
        }
        if (text.contains("差评") || text.contains("bad")) {
            return "BAD";
        }
        if (text.contains("中评") || text.contains("neutral")) {
            return "NEUTRAL";
        }
        return "GOOD";
    }

    private String ratingText(String ratingLevel) {
        if ("BAD".equals(ratingLevel)) {
            return "差评";
        }
        if ("NEUTRAL".equals(ratingLevel)) {
            return "中评";
        }
        return "好评";
    }

    private String normalizeReason(String reason) {
        if ("ORDER_FINISH".equalsIgnoreCase(trim(reason))) {
            return ORDER_FINISHED_REASON;
        }
        return reason;
    }

    private String normalizeRemark(String reason, String remark) {
        String normalizedReason = normalizeReason(reason);
        String text = trim(remark);
        if (VERIFY_APPROVED_REASON.equals(normalizedReason)) {
            return "校园认证通过";
        }
        if (ORDER_FINISHED_REASON.equals(normalizedReason)) {
            return "交易完成加分";
        }
        return text;
    }

    private String normalizeVerifyStatus(String verifyStatus) {
        String value = trim(verifyStatus).toUpperCase();
        if (value.isEmpty()) {
            return null;
        }
        return switch (value) {
            case "UNVERIFIED", "PENDING", "VERIFIED", "REJECTED" -> value;
            default -> null;
        };
    }

    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }
}


