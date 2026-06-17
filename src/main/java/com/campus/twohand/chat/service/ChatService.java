package com.campus.twohand.chat.service;

import com.campus.twohand.chat.entity.ChatMessage;
import com.campus.twohand.chat.entity.ChatSession;
import com.campus.twohand.chat.repo.ChatMessageRepository;
import com.campus.twohand.chat.repo.ChatSessionRepository;
import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.order.repo.OrdersRepository;
import com.campus.twohand.product.entity.Product;
import com.campus.twohand.product.entity.ProductImage;
import com.campus.twohand.product.repo.ProductImageRepository;
import com.campus.twohand.product.repo.ProductRepository;
import com.campus.twohand.product.support.ProductImageSanitizer;
import com.campus.twohand.product.support.ProductPriceValidator;
import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.SysUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String DEFAULT_LAST_MSG = "我想了解这个商品";

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final SysUserRepository sysUserRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final OrdersRepository ordersRepository;
    private final SessionAuthSupport sessionAuthSupport;

    @Transactional
    public ApiResp<?> createOrGetProductSession(HttpServletRequest request, Map<String, Object> body) {
        Long currentUserId = sessionAuthSupport.requireUserId(request);
        Long productId = parseLong(body.get("productId"));
        Long targetUserId = parseLong(body.get("targetUserId"));
        if (productId == null) {
            return ApiResp.fail("商品不存在");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        SysUser currentUser = requireUser(currentUserId);
        boolean currentUserIsSeller = Objects.equals(currentUserId, product.getSellerId());
        if (currentUserIsSeller && targetUserId == null) {
            throw new RuntimeException("不能联系自己发布的商品");
        }
        requireSameSchool(currentUser, product);
        if (targetUserId == null && !currentUserIsSeller && (!"APPROVED".equalsIgnoreCase(product.getAuditStatus())
                || !"ON".equalsIgnoreCase(product.getStatus())
                || isProductSold(product.getId()))) {
            throw new RuntimeException("商品当前不可发起聊天");
        }

        Long otherUserId = currentUserIsSeller ? targetUserId : product.getSellerId();
        if (Objects.equals(currentUserId, otherUserId)) {
            throw new RuntimeException("不能联系自己发布的商品");
        }
        SysUser otherUser = requireUser(otherUserId);
        Long userLow = Math.min(currentUserId, otherUser.getId());
        Long userHigh = Math.max(currentUserId, otherUser.getId());
        ChatSession session = chatSessionRepository
                .findByUserLowAndUserHighAndProductId(userLow, userHigh, product.getId())
                .orElseGet(() -> {
                    ChatSession created = new ChatSession();
                    created.setUserLow(userLow);
                    created.setUserHigh(userHigh);
                    created.setProductId(product.getId());
                    created.setLastMsg(DEFAULT_LAST_MSG);
                    created.setLastTime(LocalDateTime.now());
                    return chatSessionRepository.save(created);
                });

        return ApiResp.ok(toSessionView(session, currentUserId));
    }

    @Transactional
    public ApiResp<?> sendMessage(HttpServletRequest request, Map<String, Object> body) {
        Long fromId = sessionAuthSupport.requireUserId(request);
        Long sessionId = parseLong(body.get("sessionId"));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content")).trim();

        if (sessionId == null || content == null || content.isEmpty()) {
            return ApiResp.fail("参数错误");
        }
        if (content.length() > 1000) {
            return ApiResp.fail("消息内容不能超过1000个字符");
        }

        ChatSession session = requireParticipantSession(sessionId, fromId);
        Long toId = otherUserId(session, fromId);

        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setFromId(fromId);
        message.setToId(toId);
        message.setContent(content);
        message.setReadFlag(0);
        chatMessageRepository.save(message);

        session.setLastMsg(content);
        session.setLastTime(message.getCreatedAt());
        chatSessionRepository.save(session);

        Map<String, Object> data = new HashMap<>();
        data.put("messageId", message.getId());
        data.put("sessionId", session.getId());
        data.put("toId", toId);
        data.put("createdAt", message.getCreatedAt());
        return ApiResp.ok(data);
    }

    @Transactional
    public ApiResp<?> getChatHistory(HttpServletRequest request, Long sessionId, int page, int size) {
        Long currentUserId = sessionAuthSupport.requireUserId(request);
        ChatSession session = requireParticipantSession(sessionId, currentUserId);

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtDesc(session.getId(), pageable);

        List<Map<String, Object>> list = new ArrayList<>();
        for (ChatMessage msg : messages.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", msg.getId());
            item.put("sessionId", msg.getSessionId());
            item.put("fromId", msg.getFromId());
            item.put("toId", msg.getToId());
            item.put("content", msg.getContent());
            item.put("readFlag", msg.getReadFlag());
            item.put("createdAt", msg.getCreatedAt());
            list.add(item);
        }
        chatMessageRepository.markSessionMessagesRead(session.getId(), currentUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("records", list);
        data.put("total", messages.getTotalElements());
        data.put("session", toSessionView(session, currentUserId));
        return ApiResp.ok(data);
    }

    public ApiResp<?> getContacts(HttpServletRequest request) {
        Long currentUserId = sessionAuthSupport.requireUserId(request);
        List<Map<String, Object>> contacts = new ArrayList<>();
        for (ChatSession session : chatSessionRepository.findUserSessions(currentUserId)) {
            contacts.add(toSessionView(session, currentUserId));
        }
        return ApiResp.ok(contacts);
    }

    @Transactional
    public ApiResp<?> markAsRead(HttpServletRequest request, Long sessionId) {
        Long currentUserId = sessionAuthSupport.requireUserId(request);
        ChatSession session = requireParticipantSession(sessionId, currentUserId);
        int updated = chatMessageRepository.markSessionMessagesRead(session.getId(), currentUserId);
        return ApiResp.ok("已标记 " + updated + " 条消息为已读");
    }

    public ApiResp<?> getUnreadCount(HttpServletRequest request) {
        Long currentUserId = sessionAuthSupport.requireUserId(request);
        return ApiResp.ok(chatMessageRepository.countByToIdAndReadFlag(currentUserId, 0));
    }

    @Transactional
    public ApiResp<?> deleteConversation(HttpServletRequest request, Long sessionId) {
        Long currentUserId = sessionAuthSupport.requireUserId(request);
        ChatSession session = requireParticipantSession(sessionId, currentUserId);
        int deleted = chatMessageRepository.deleteBySessionIdForConversation(session.getId());
        chatSessionRepository.delete(session);
        return ApiResp.ok(deleted);
    }

    public ApiResp<?> getChatProduct(HttpServletRequest request, Long productId, Long sessionId) {
        Long currentUserId = sessionAuthSupport.requireUserId(request);
        if (sessionId != null) {
            ChatSession session = requireParticipantSession(sessionId, currentUserId);
            if (session.getProductId() == null || !session.getProductId().equals(productId)) {
                throw new RuntimeException("无权限查看该商品");
            }
            Product product = productRepository.findById(session.getProductId())
                    .orElseThrow(() -> new RuntimeException("商品不存在"));
            return ApiResp.ok(toProductCard(product, currentUserId));
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        requireSameSchool(requireUser(currentUserId), product);
        if (!"APPROVED".equalsIgnoreCase(product.getAuditStatus())) {
            throw new RuntimeException("商品暂不可查看");
        }
        return ApiResp.ok(toProductCard(product, currentUserId));
    }

    @Transactional
    public ApiResp<?> updateChatProductPrice(HttpServletRequest request, Map<String, Object> body) {
        Long userId = sessionAuthSupport.requireUserId(request);
        Long productId = parseLong(body.get("productId"));
        if (productId == null) {
            throw new RuntimeException("商品不存在");
        }

        BigDecimal price = ProductPriceValidator.validate(body.get("price"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        if (!userId.equals(product.getSellerId())) {
            throw new RuntimeException("只有卖家可以修改价格");
        }
        if (!"APPROVED".equals(product.getAuditStatus())) {
            throw new RuntimeException("商品未通过审核");
        }
        if (!"ON".equals(product.getStatus())) {
            throw new RuntimeException("商品当前不可改价");
        }
        if (isProductSold(product.getId())) {
            throw new RuntimeException("商品已售出，无法改价");
        }

        product.setPrice(price);
        productRepository.save(product);
        return ApiResp.ok(toProductCard(product, userId));
    }

    private Map<String, Object> toSessionView(ChatSession session, Long currentUserId) {
        Long otherId = otherUserId(session, currentUserId);
        SysUser otherUser = sysUserRepository.findById(otherId).orElse(null);

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", session.getId());
        data.put("id", session.getId());
        data.put("otherUserId", otherId);
        data.put("otherUserName", otherUser == null ? "用户" + otherId : resolveDisplayName(otherUser));
        data.put("otherUserAvatar", otherUser == null ? "" : otherUser.getAvatar());
        data.put("name", otherUser == null ? "用户" + otherId : resolveDisplayName(otherUser));
        data.put("avatar", otherUser == null ? "" : otherUser.getAvatar());
        data.put("productId", session.getProductId());
        data.put("lastMsg", session.getLastMsg());
        data.put("lastMessage", session.getLastMsg());
        data.put("lastTime", session.getLastTime());
        data.put("unreadCount", chatMessageRepository.countBySessionIdAndToIdAndReadFlag(session.getId(), currentUserId, 0));

        if (session.getProductId() != null) {
            productRepository.findById(session.getProductId()).ifPresent(product -> {
                Map<String, Object> productCard = toProductCard(product, currentUserId);
                data.put("productCard", productCard);
                data.put("productTitle", productCard.get("productTitle"));
                data.put("productPrice", productCard.get("productPrice"));
                data.put("productCover", productCard.get("productCover"));
                data.put("productStatus", productCard.get("productStatus"));
                data.put("canBuy", productCard.get("canBuy"));
                data.put("buyDisabledReason", productCard.get("buyDisabledReason"));
                data.put("isSeller", productCard.get("isSeller"));
                data.put("isOwnProduct", productCard.get("isOwnProduct"));
            });
        }
        return data;
    }

    private Map<String, Object> toProductCard(Product product, Long currentUserId) {
        Map<String, Object> data = new HashMap<>();
        boolean sold = isProductSold(product.getId());
        boolean approved = "APPROVED".equalsIgnoreCase(product.getAuditStatus());
        boolean on = "ON".equalsIgnoreCase(product.getStatus());
        boolean owner = Objects.equals(currentUserId, product.getSellerId());
        boolean sameSchool = isSameSchool(currentUserId, product);
        boolean canBuy = approved && on && !sold && !owner && sameSchool;
        String reason;
        if (!sameSchool) {
            reason = "非本校商品";
        } else if (!approved) {
            reason = "暂不可购买";
        } else if (sold) {
            reason = "已售出";
        } else if (!on) {
            reason = "商品已下架";
        } else if (owner) {
            reason = "自己的商品";
        } else {
            reason = "立即购买";
        }

        if (!sameSchool) {
            reason = "\u975e\u672c\u6821\u5546\u54c1";
        } else if (!approved) {
            reason = "\u6682\u4e0d\u53ef\u8d2d\u4e70";
        } else if (sold) {
            reason = "\u5df2\u552e\u51fa";
        } else if (!on) {
            reason = "\u5546\u54c1\u5df2\u4e0b\u67b6";
        } else if (owner) {
            reason = "\u81ea\u5df1\u7684\u5546\u54c1";
        } else {
            reason = "\u7acb\u5373\u8d2d\u4e70";
        }

        data.put("id", product.getId());
        data.put("productId", product.getId());
        data.put("title", product.getTitle());
        data.put("productTitle", product.getTitle());
        data.put("price", product.getPrice());
        data.put("productPrice", product.getPrice());
        data.put("coverUrl", resolveCover(product.getId()));
        data.put("productCover", resolveCover(product.getId()));
        data.put("status", product.getStatus());
        data.put("auditStatus", product.getAuditStatus());
        data.put("productStatus", sold ? "SOLD" : product.getStatus());
        data.put("sellerId", product.getSellerId());
        data.put("isSeller", owner);
        data.put("isOwnProduct", owner);
        data.put("canBuy", canBuy);
        data.put("buyDisabledReason", reason);
        return data;
    }

    private String resolveCover(Long productId) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortAscIdAsc(productId);
        if (images == null || images.isEmpty()) {
            return ProductImageSanitizer.DEFAULT_PRODUCT_PLACEHOLDER;
        }
        return ProductImageSanitizer.coverOrPlaceholder(images.get(0).getUrl());
    }

    private ChatSession requireParticipantSession(Long sessionId, Long userId) {
        if (sessionId == null) {
            throw new RuntimeException("会话不存在");
        }
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        if (!Objects.equals(session.getUserLow(), userId) && !Objects.equals(session.getUserHigh(), userId)) {
            throw new RuntimeException("无权限查看该会话");
        }
        return session;
    }

    private Long otherUserId(ChatSession session, Long userId) {
        return Objects.equals(session.getUserLow(), userId) ? session.getUserHigh() : session.getUserLow();
    }

    private boolean isProductSold(Long productId) {
        return ordersRepository.existsByProductIdAndStatusNot(productId, "CANCELLED");
    }

    private void requireSameSchool(SysUser user, Product product) {
        String userSchool = trimToNull(user == null ? null : user.getSchool());
        String productSchool = trimToNull(product == null ? null : product.getSchoolName());
        if (userSchool == null || productSchool == null || !userSchool.equals(productSchool)) {
            throw new RuntimeException("该商品不属于当前学校");
        }
    }

    private boolean isSameSchool(Long userId, Product product) {
        if (userId == null || product == null) {
            return false;
        }
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        String userSchool = trimToNull(user == null ? null : user.getSchool());
        String productSchool = trimToNull(product.getSchoolName());
        return userSchool != null && userSchool.equals(productSchool);
    }

    private SysUser requireUser(Long userId) {
        return sysUserRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String resolveDisplayName(SysUser user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return "\u6821\u56ed\u7528\u6237";
    }
}
