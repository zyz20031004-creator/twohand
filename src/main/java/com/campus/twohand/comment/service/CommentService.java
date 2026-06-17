package com.campus.twohand.comment.service;

import com.campus.twohand.comment.entity.Comment;
import com.campus.twohand.comment.repo.CommentRepository;
import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.order.repo.OrdersRepository;
import com.campus.twohand.product.entity.Product;
import com.campus.twohand.product.entity.ProductLike;
import com.campus.twohand.product.repo.ProductLikeRepository;
import com.campus.twohand.product.repo.ProductRepository;
import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.SysUserRepository;
import com.campus.twohand.wanted.entity.Wanted;
import com.campus.twohand.wanted.repo.WantedRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final String PRODUCT = "PRODUCT";
    private static final String WANTED = "WANTED";

    private final CommentRepository commentRepository;
    private final SessionAuthSupport sessionAuthSupport;
    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;
    private final WantedRepository wantedRepository;
    private final SysUserRepository sysUserRepository;
    private final OrdersRepository ordersRepository;

    public ApiResp<?> listProduct(Long productId) {
        List<Map<String, Object>> list = commentRepository.listProductCommentsWithUser(productId);
        Map<String, Object> data = new HashMap<>();
        data.put("records", list);
        data.put("total", list.size());
        return ApiResp.ok(data);
    }

    public ApiResp<?> addProduct(HttpServletRequest request, Long productId, Long parentId, String content) {
        return addComment(request, PRODUCT, productId, parentId, content, "productId");
    }

    public ApiResp<?> productPage(HttpServletRequest request,
                                  Long productId,
                                  int page,
                                  int size,
                                  Long currentUserId) {
        Long viewerId = resolveViewerId(request, currentUserId);
        List<Map<String, Object>> all = commentRepository.listProductCommentsWithUser(productId);

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : all) {
            Object statusObj = row.get("status");
            if (statusObj == null || Integer.parseInt(String.valueOf(statusObj)) == 1) {
                filtered.add(row);
            }
        }

        int total = filtered.size();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int from = (safePage - 1) * safeSize;
        int to = Math.min(from + safeSize, total);

        List<Map<String, Object>> slice = from >= total ? Collections.emptyList() : filtered.subList(from, to);
        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : slice) {
            Map<String, Object> item = new HashMap<>(row);
            Long authorId = parseLong(item.get("userId"));
            boolean mine = viewerId != null && authorId != null && authorId.equals(viewerId);
            item.put("mine", mine);
            item.put("canDelete", mine);
            records.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", total);
        data.put("page", safePage);
        data.put("size", safeSize);
        return ApiResp.ok(data);
    }

    public ApiResp<?> deleteProductComment(HttpServletRequest request, Long id) {
        return deleteOwnComment(request, id, PRODUCT);
    }

    public ApiResp<?> toggleLike(HttpServletRequest request, Long productId) {
        Long currentUserId = sessionAuthSupport.requireUserId(request);
        var opt = productLikeRepository.findByUserIdAndProductId(currentUserId, productId);

        int newStatus;
        if (opt.isPresent()) {
            ProductLike like = opt.get();
            newStatus = like.getStatus() != null && like.getStatus() == 1 ? 0 : 1;
            like.setStatus(newStatus);
            productLikeRepository.save(like);
        } else {
            ProductLike like = new ProductLike();
            like.setUserId(currentUserId);
            like.setProductId(productId);
            like.setStatus(1);
            productLikeRepository.save(like);
            newStatus = 1;
        }

        long likeCount = productLikeRepository.countByProductIdAndStatus(productId, 1);
        Map<String, Object> data = new HashMap<>();
        data.put("liked", newStatus == 1);
        data.put("likeCount", likeCount);
        return ApiResp.ok(data);
    }

    public ApiResp<?> productTree(HttpServletRequest request, Long productId, Long currentUserId) {
        Long viewerId = resolveViewerId(request, currentUserId);
        List<Map<String, Object>> rows = commentRepository.listProductCommentsWithUser(productId);
        Map<String, Object> data = new HashMap<>();
        data.put("records", buildTree(rows, viewerId));
        data.put("total", rows.size());
        return ApiResp.ok(data);
    }

    public ApiResp<?> wantedList(Long targetId) {
        List<Object[]> rows = commentRepository.listWantedCommentsWithUser(targetId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[0]);
            item.put("userId", row[1]);
            item.put("content", row[2]);
            item.put("createdAt", row[3]);
            item.put("nickname", row[4]);
            item.put("avatar", row[5]);
            item.put("avatarUrl", row[5]);
            list.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", list);
        data.put("total", list.size());
        return ApiResp.ok(data);
    }

    public ApiResp<?> wantedTree(HttpServletRequest request, Long targetId, Long currentUserId) {
        Long viewerId = resolveViewerId(request, currentUserId);
        List<Map<String, Object>> rows = commentRepository.listWantedCommentsWithUserMap(targetId);
        Map<String, Object> data = new HashMap<>();
        data.put("records", buildTree(rows, viewerId));
        data.put("total", rows.size());
        return ApiResp.ok(data);
    }

    public ApiResp<?> addWanted(HttpServletRequest request, Long targetId, Long parentId, String content) {
        return addComment(request, WANTED, targetId, parentId, content, "targetId");
    }

    public ApiResp<?> deleteWantedComment(HttpServletRequest request, Long id) {
        return deleteOwnComment(request, id, WANTED);
    }

    private ApiResp<?> addComment(HttpServletRequest request,
                                  String targetType,
                                  Long targetId,
                                  Long parentId,
                                  String content,
                                  String idFieldName) {
        if (targetId == null) {
            return ApiResp.fail("缺少 " + idFieldName);
        }
        if (content == null || content.trim().isEmpty()) {
            return ApiResp.fail("评论内容不能为空");
        }

        Long currentUserId = sessionAuthSupport.requireUserId(request);
        validateCommentTarget(currentUserId, targetType, targetId);
        Comment comment = new Comment();
        comment.setTargetType(targetType);
        comment.setTargetId(targetId);
        comment.setUserId(currentUserId);
        comment.setContent(content.trim());
        comment.setStatus(1);
        comment.setLikeCount(0);
        comment.setParentId(parentId);
        commentRepository.save(comment);
        return ApiResp.ok("ok");
    }

    private ApiResp<?> deleteOwnComment(HttpServletRequest request, Long id, String targetType) {
        Long currentUserId = sessionAuthSupport.requireUserId(request);
        Comment comment = commentRepository.findById(id).orElse(null);
        if (comment == null) {
            return ApiResp.fail("评论不存在");
        }
        if (!targetType.equals(comment.getTargetType())) {
            return ApiResp.fail("评论类型不匹配");
        }
        if (!Objects.equals(currentUserId, comment.getUserId())) {
            return ApiResp.fail("只能删除自己的评论");
        }

        comment.setStatus(0);
        commentRepository.save(comment);
        return ApiResp.ok("ok");
    }

    private List<Map<String, Object>> buildTree(List<Map<String, Object>> rows, Long viewerId) {
        Map<Long, Map<String, Object>> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<>(row);
            item.put("children", new ArrayList<>());

            Long authorId = parseLong(item.get("userId"));
            boolean canDelete = viewerId != null && authorId != null && authorId.equals(viewerId);
            item.put("canDelete", canDelete);
            item.put("mine", canDelete);
            map.put(parseLong(item.get("id")), item);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> item : map.values()) {
            Long parentId = parseLong(item.get("parentId"));
            if (parentId == null) {
                roots.add(item);
                continue;
            }

            Map<String, Object> parent = map.get(parentId);
            if (parent == null) {
                roots.add(item);
                continue;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
            children.add(item);
        }
        return roots;
    }

    private Long resolveViewerId(HttpServletRequest request, Long fallbackUserId) {
        Long sessionUserId = sessionAuthSupport.currentUserId(request);
        return sessionUserId != null ? sessionUserId : fallbackUserId;
    }

    private void validateCommentTarget(Long userId, String targetType, Long targetId) {
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (isAdmin(user)) {
            validateTargetExists(targetType, targetId);
            return;
        }

        String userSchool = verifiedSchoolOf(user);
        if (PRODUCT.equals(targetType)) {
            Product product = productRepository.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("商品不存在"));
            String productSchool = trimToNull(product.getSchoolName());
            if (productSchool == null || !userSchool.equals(productSchool)) {
                throw new RuntimeException("该商品不属于当前学校，无法评论");
            }
            if (!"APPROVED".equals(product.getAuditStatus())
                    || !"ON".equals(product.getStatus())
                    || ordersRepository.existsByProductIdAndStatusNot(product.getId(), "CANCELLED")) {
                throw new RuntimeException("该商品暂不可评论");
            }
            return;
        }

        if (WANTED.equals(targetType)) {
            Wanted wanted = wantedRepository.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("求购不存在"));
            String wantedSchool = trimToNull(wanted.getSchoolName());
            if (wantedSchool == null || !userSchool.equals(wantedSchool)) {
                throw new RuntimeException("该求购不属于当前学校，无法评论");
            }
            if (!"OPEN".equals(wanted.getStatus())) {
                throw new RuntimeException("该求购暂不可评论");
            }
            return;
        }

        throw new RuntimeException("评论类型不支持");
    }

    private void validateTargetExists(String targetType, Long targetId) {
        if (PRODUCT.equals(targetType) && !productRepository.existsById(targetId)) {
            throw new RuntimeException("商品不存在");
        }
        if (WANTED.equals(targetType) && !wantedRepository.existsById(targetId)) {
            throw new RuntimeException("求购不存在");
        }
    }

    private String verifiedSchoolOf(SysUser user) {
        String school = trimToNull(user == null ? null : user.getSchool());
        if (user == null || !"VERIFIED".equalsIgnoreCase(user.getVerifyStatus()) || school == null) {
            throw new RuntimeException("请先完成学号认证");
        }
        return school;
    }

    private boolean isAdmin(SysUser user) {
        return user != null
                && ("ADMIN".equalsIgnoreCase(user.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(user.getRole()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
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
}
