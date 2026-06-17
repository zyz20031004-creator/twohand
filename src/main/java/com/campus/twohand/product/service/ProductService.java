package com.campus.twohand.product.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.order.entity.Orders;
import com.campus.twohand.order.repo.OrdersRepository;
import com.campus.twohand.product.entity.Favorite;
import com.campus.twohand.product.entity.Product;
import com.campus.twohand.product.entity.ProductImage;
import com.campus.twohand.product.entity.ProductLike;
import com.campus.twohand.product.repo.FavoriteRepository;
import com.campus.twohand.product.repo.ProductImageRepository;
import com.campus.twohand.product.repo.ProductLikeRepository;
import com.campus.twohand.product.repo.ProductRepository;
import com.campus.twohand.product.support.ProductImageSanitizer;
import com.campus.twohand.product.support.ProductPriceValidator;
import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.SysUserRepository;
import com.campus.twohand.verify.service.VerifyGuard;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int MAX_PRODUCT_IMAGE_COUNT = 6;
    private static final int MAX_PRODUCT_IMAGE_URL_LENGTH = 255;
    private static final int MAX_PRODUCT_ADDRESS_TEXT_LENGTH = 100;
    private static final Pattern MOBILE_PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final FavoriteRepository favoriteRepository;
    private final ProductLikeRepository productLikeRepository;
    private final OrdersRepository ordersRepository;
    private final SysUserRepository sysUserRepository;
    private final VerifyGuard verifyGuard;
    private final SessionAuthSupport sessionAuthSupport;

    public ApiResp<?> page(HttpServletRequest request,
                           int page,
                           int size,
                           String keyword,
                           Long categoryId,
                           String status,
                           String auditStatus,
                           String sortBy,
                           String sortOrder) {
        String currentSchool = resolveBrowseSchool(request);
        Sort sort = buildSort(sortBy, sortOrder);
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + keyword + "%"));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (auditStatus != null && !auditStatus.isEmpty()) {
                predicates.add(cb.equal(root.get("auditStatus"), auditStatus));
            }
            if (currentSchool != null) {
                predicates.add(cb.equal(root.get("schoolName"), currentSchool));
            }
            var sellerSubquery = query.subquery(Long.class);
            var sellerRoot = sellerSubquery.from(SysUser.class);
            sellerSubquery.select(sellerRoot.get("id"));
            sellerSubquery.where(
                    cb.equal(sellerRoot.get("id"), root.get("sellerId")),
                    cb.equal(sellerRoot.get("verifyStatus"), "VERIFIED")
            );
            predicates.add(cb.exists(sellerSubquery));
            var activeOrderSubquery = query.subquery(Long.class);
            var orderRoot = activeOrderSubquery.from(Orders.class);
            activeOrderSubquery.select(orderRoot.get("id"));
            activeOrderSubquery.where(
                    cb.equal(orderRoot.get("productId"), root.get("id")),
                    cb.notEqual(orderRoot.get("status"), "CANCELLED")
            );
            predicates.add(cb.not(cb.exists(activeOrderSubquery)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<Product> products = productPage.getContent();
        if (products.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("records", Collections.emptyList());
            result.put("total", productPage.getTotalElements());
            result.put("page", page);
            result.put("size", size);
            return ApiResp.ok(result);
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, Long> likeCountMap = new HashMap<>();
        for (var row : productLikeRepository.countLikesByProductIds(productIds)) {
            likeCountMap.put(row.getProductId(), row.getCnt());
        }

        Map<Long, Long> favoriteCountMap = new HashMap<>();
        for (Map<String, Object> row : favoriteRepository.countFavoritesByProductIds(productIds)) {
            Long productId = ((Number) row.get("productId")).longValue();
            Long count = ((Number) row.get("cnt")).longValue();
            favoriteCountMap.put(productId, count);
        }

        Map<Long, List<ProductImage>> imageMap = loadProductImageMap(productIds);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Product product : products) {
            List<ProductImage> productImages = imageMap.getOrDefault(product.getId(), Collections.emptyList());
            Map<String, Object> item = new HashMap<>();
            item.put("id", product.getId());
            item.put("title", product.getTitle());
            item.put("price", product.getPrice());
            item.put("viewCount", product.getViewCount());
            item.put("likeCount", likeCountMap.getOrDefault(product.getId(), 0L));
            item.put("favoriteCount", favoriteCountMap.getOrDefault(product.getId(), 0L));
            item.put("coverUrl", resolveCoverUrl(productImages));
            item.put("images", productImages);
            item.put("schoolName", product.getSchoolName());
            records.add(item);
        }

        return buildPagedResp(page, size, productPage, records);
    }

    public ApiResp<?> detail(HttpServletRequest request, Long id, Long userId) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ApiResp.fail("商品不存在");
        }

        ensureProductVisible(request, product);

        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);

        List<ProductImage> images = sanitizeImages(productImageRepository.findByProductIdOrderBySortAscIdAsc(id));
        String coverUrl = resolveCoverUrl(images);
        long favoriteCount = favoriteRepository.countByProductIdAndStatus(id, 1);
        long likeCount = productLikeRepository.countByProductIdAndStatus(id, 1);

        Long currentUserId = sessionAuthSupport.currentUserId(request);
        if (currentUserId == null) {
            currentUserId = userId;
        }

        boolean favorited = false;
        boolean liked = false;
        if (currentUserId != null) {
            favorited = favoriteRepository.findByUserIdAndProductId(currentUserId, id)
                    .map(favorite -> favorite.getStatus() == 1)
                    .orElse(false);
            liked = productLikeRepository.findByUserIdAndProductId(currentUserId, id)
                    .map(like -> like.getStatus() == 1)
                    .orElse(false);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", product.getId());
        data.put("title", product.getTitle());
        data.put("price", product.getPrice());
        data.put("description", product.getDescription());
        data.put("addressText", product.getAddressText());
        data.put("schoolName", product.getSchoolName());
        data.put("createdAt", product.getCreatedAt());
        data.put("viewCount", product.getViewCount());
        data.put("likeCount", likeCount);
        data.put("liked", liked);
        data.put("coverUrl", coverUrl);
        data.put("images", images);
        data.put("favoriteCount", favoriteCount);
        data.put("favorited", favorited);
        data.put("status", product.getStatus());
        data.put("auditStatus", product.getAuditStatus());
        if (Objects.equals(currentUserId, product.getSellerId())) {
            data.put("auditReason", product.getAuditReason());
        }

        var sellerOpt = sysUserRepository.findById(product.getSellerId());
        String sellerName = sellerOpt
                .map(this::resolvePublicDisplayName)
                .orElse("\u6821\u56ed\u5356\u5bb6");
        String sellerAvatar = sellerOpt.map(SysUser::getAvatar).orElse(null);

        data.put("sellerId", product.getSellerId());
        data.put("sellerName", sellerName);
        data.put("sellerAvatar", sellerAvatar);
        return ApiResp.ok(data);
    }

    public ApiResp<?> toggleFavorite(HttpServletRequest request, Long productId) {
        Long userId = sessionAuthSupport.requireUserId(request);
        var favoriteOpt = favoriteRepository.findByUserIdAndProductId(userId, productId);

        int newStatus;
        if (favoriteOpt.isPresent()) {
            Favorite favorite = favoriteOpt.get();
            newStatus = favorite.getStatus() != null && favorite.getStatus() == 1 ? 0 : 1;
            favorite.setStatus(newStatus);
            favoriteRepository.save(favorite);
        } else {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setProductId(productId);
            favorite.setStatus(1);
            favoriteRepository.save(favorite);
            newStatus = 1;
        }

        long favoriteCount = favoriteRepository.countByProductIdAndStatus(productId, 1);
        Map<String, Object> data = new HashMap<>();
        data.put("favorited", newStatus == 1);
        data.put("favoriteCount", favoriteCount);
        return ApiResp.ok(data);
    }

    public ApiResp<?> toggleLike(HttpServletRequest request, Long productId) {
        Long userId = sessionAuthSupport.requireUserId(request);
        var likeOpt = productLikeRepository.findByUserIdAndProductId(userId, productId);

        int newStatus;
        if (likeOpt.isPresent()) {
            ProductLike like = likeOpt.get();
            newStatus = like.getStatus() != null && like.getStatus() == 1 ? 0 : 1;
            like.setStatus(newStatus);
            productLikeRepository.save(like);
        } else {
            ProductLike like = new ProductLike();
            like.setUserId(userId);
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

    public ApiResp<?> myPage(HttpServletRequest request, int page, int size, String keyword, String status) {
        Long userId = sessionAuthSupport.requireUserId(request);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("sellerId"), userId));
            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + keyword + "%"));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<Product> products = productPage.getContent();
        Map<Long, List<ProductImage>> imageMap = loadProductImageMap(products.stream().map(Product::getId).toList());
        List<Map<String, Object>> records = new ArrayList<>();
        for (Product product : products) {
            List<ProductImage> images = imageMap.getOrDefault(product.getId(), Collections.emptyList());
            Map<String, Object> item = new HashMap<>();
            item.put("id", product.getId());
            item.put("title", product.getTitle());
            item.put("price", product.getPrice());
            item.put("status", product.getStatus());
            item.put("auditStatus", product.getAuditStatus());
            item.put("auditReason", product.getAuditReason());
            item.put("createdAt", product.getCreatedAt());
            item.put("categoryId", product.getCategoryId());
            item.put("description", product.getDescription());
            item.put("addressText", product.getAddressText());
            item.put("schoolName", product.getSchoolName());
            item.put("coverUrl", resolveCoverUrl(images));
            item.put("images", images);
            records.add(item);
        }

        return buildPagedResp(page, size, productPage, records);
    }

    public ApiResp<?> toggle(HttpServletRequest request, Long id) {
        Long userId = sessionAuthSupport.requireUserId(request);
        Product product = requireOwnedProduct(id, userId);

        if ("OFF".equals(product.getStatus())
                && ordersRepository.existsByProductIdAndStatusNot(id, "CANCELLED")) {
            throw new RuntimeException("该商品已有有效订单，无法重新上架");
        }

        if ("ON".equals(product.getStatus())) {
            product.setStatus("OFF");
        } else {
            product.setAuditStatus("PENDING");
            product.setAuditReason(null);
            product.setStatus("OFF");
        }
        productRepository.saveAndFlush(product);
        return ApiResp.ok(null);
    }

    @Transactional
    public ApiResp<?> update(HttpServletRequest request, Long id, Map<String, Object> req) {
        Long userId = sessionAuthSupport.requireUserId(request);
        Product product = requireOwnedProduct(id, userId);
        rejectIfSoldProduct(id);

        applyProductFields(product, req);
        product.setAuditStatus("PENDING");
        product.setAuditReason(null);
        product.setStatus("OFF");
        productRepository.save(product);
        syncProductImages(product.getId(), parseImageUrls(req));
        return ApiResp.ok(null);
    }

    @Transactional
    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        Long userId = sessionAuthSupport.requireUserId(request);
        requireOwnedProduct(id, userId);

        productImageRepository.deleteByProductId(id);
        productRepository.deleteById(id);
        return ApiResp.ok(null);
    }

    @Transactional
    public ApiResp<?> create(HttpServletRequest request, Map<String, Object> req) {
        Long userId = sessionAuthSupport.requireUserId(request);
        verifyGuard.requireVerified(userId);
        SysUser seller = requireVerifiedSellerWithSchool(userId);

        Product product = new Product();
        applyProductFields(product, req);
        product.setSellerId(userId);
        product.setSchoolName(seller.getSchool().trim());
        product.setAuditStatus("PENDING");
        product.setViewCount(0);
        product.setLikeCount(0);
        product.setCreatedAt(LocalDateTime.now());

        productRepository.save(product);
        syncProductImages(product.getId(), parseImageUrls(req));

        return ApiResp.ok(null);
    }

    private void applyProductFields(Product product, Map<String, Object> req) {
        String title = trimToNull(req.get("title"));
        if (title == null) {
            throw new RuntimeException("商品标题不能为空");
        }

        BigDecimal price = ProductPriceValidator.validate(req.get("price"));

        String status = trimToNull(req.get("status"));
        if (status == null) {
            status = "OFF";
        }

        product.setTitle(title);
        product.setPrice(price);
        product.setStatus(status);
        product.setDescription(trimToNull(req.get("description")));
        product.setAddressText(validateProductAddressText(req.get("addressText")));
        product.setCategoryId(parseLong(req.get("categoryId")));
    }

    private String validateProductAddressText(Object value) {
        String text = trimToNull(value);
        if (text == null) {
            throw new RuntimeException("交易方式不能为空");
        }
        if (text.length() > MAX_PRODUCT_ADDRESS_TEXT_LENGTH) {
            throw new RuntimeException("交易地点不能超过 100 个字符");
        }
        if (text.startsWith("历史地址：") || text.startsWith("历史地址:")) {
            throw new RuntimeException("交易地点不能包含历史地址前缀");
        }
        if (MOBILE_PHONE_PATTERN.matcher(text).find()) {
            throw new RuntimeException("交易地点不能包含手机号");
        }
        long separatorCount = text.chars().filter(ch -> ch == '|' || ch == '｜').count();
        if (separatorCount >= 2 && (text.contains("默认地址") || text.contains("手机号") || text.contains("电话"))) {
            throw new RuntimeException("交易地点不能保存完整地址簿信息");
        }
        return text;
    }

    private Map<Long, List<ProductImage>> loadProductImageMap(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ProductImage> images = productImageRepository.findByProductIdInOrderByProductIdAscSortAscIdAsc(productIds);
        Map<Long, List<ProductImage>> imageMap = new HashMap<>();
        for (ProductImage image : images) {
            if (sanitizeImageUrl(image.getUrl()) == null) {
                continue;
            }
            imageMap.computeIfAbsent(image.getProductId(), key -> new ArrayList<>()).add(image);
        }
        return imageMap;
    }

    private String resolveCoverUrl(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return ProductImageSanitizer.DEFAULT_PRODUCT_PLACEHOLDER;
        }
        return ProductImageSanitizer.coverOrPlaceholder(images.get(0).getUrl());
    }

    private List<String> parseImageUrls(Map<String, Object> req) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        Object images = req.get("images");
        if (images instanceof List<?> imageList) {
            for (Object item : imageList) {
                String url = extractImageUrl(item);
                if (url != null) {
                    urls.add(url);
                }
            }
        }

        if (urls.isEmpty()) {
            String coverUrl = sanitizeImageUrl(req.get("coverUrl"));
            if (coverUrl != null) {
                urls.add(coverUrl);
            }
        }

        return new ArrayList<>(urls);
    }

    private String extractImageUrl(Object image) {
        if (image instanceof String text) {
            return sanitizeImageUrl(text);
        }
        if (image instanceof Map<?, ?> map) {
            Object url = map.get("url");
            return sanitizeImageUrl(url);
        }
        return null;
    }

    private List<ProductImage> sanitizeImages(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductImage> validImages = new ArrayList<>();
        for (ProductImage image : images) {
            if (sanitizeImageUrl(image.getUrl()) != null) {
                validImages.add(image);
            }
        }
        return validImages;
    }

    private String sanitizeImageUrl(Object value) {
        return ProductImageSanitizer.sanitize(trimToNull(value));
    }

    private void rejectIfSoldProduct(Long productId) {
        if (ordersRepository.existsByProductIdAndStatus(productId, "FINISHED")) {
            throw new RuntimeException("已售出的商品不允许编辑");
        }
    }

    private void syncProductImages(Long productId, List<String> imageUrls) {
        List<String> nextUrls = normalizeImageUrls(imageUrls);
        List<ProductImage> existingImages = productImageRepository.findByProductIdOrderBySortAscIdAsc(productId);

        Map<String, ProductImage> existingByUrl = new LinkedHashMap<>();
        List<ProductImage> imagesToDelete = new ArrayList<>();
        for (ProductImage image : existingImages) {
            String url = sanitizeImageUrl(image.getUrl());
            if (url == null) {
                imagesToDelete.add(image);
                continue;
            }
            if (existingByUrl.containsKey(url)) {
                imagesToDelete.add(image);
                continue;
            }
            existingByUrl.put(url, image);
        }

        Set<String> nextUrlSet = new LinkedHashSet<>(nextUrls);
        for (Map.Entry<String, ProductImage> entry : existingByUrl.entrySet()) {
            if (!nextUrlSet.contains(entry.getKey())) {
                imagesToDelete.add(entry.getValue());
            }
        }

        List<ProductImage> imagesToInsert = new ArrayList<>();
        for (int i = 0; i < nextUrls.size(); i++) {
            String url = nextUrls.get(i);
            int nextSort = i + 1;
            ProductImage existing = existingByUrl.get(url);
            if (existing != null) {
                if (!Objects.equals(existing.getSort(), nextSort)) {
                    existing.setSort(nextSort);
                }
                continue;
            }

            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setUrl(url);
            image.setSort(nextSort);
            imagesToInsert.add(image);
        }

        if (!imagesToDelete.isEmpty()) {
            productImageRepository.deleteAll(imagesToDelete);
        }
        if (!imagesToInsert.isEmpty()) {
            productImageRepository.saveAll(imagesToInsert);
        }
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String imageUrl : imageUrls) {
            String url = sanitizeImageUrl(imageUrl);
            if (url != null) {
                if (url.length() > MAX_PRODUCT_IMAGE_URL_LENGTH) {
                    throw new RuntimeException("商品图片地址长度不能超过 255");
                }
                normalized.add(url);
            }
        }
        if (normalized.size() > MAX_PRODUCT_IMAGE_COUNT) {
            throw new RuntimeException("商品图片最多上传 6 张");
        }
        return new ArrayList<>(normalized);
    }

    private String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String resolvePublicDisplayName(SysUser user) {
        String name = user == null ? null : trimToNull(user.getName());
        return name == null ? "\u6821\u56ed\u5356\u5bb6" : name;
    }

    private String resolveBrowseSchool(HttpServletRequest request) {
        Long userId = sessionAuthSupport.currentUserId(request);
        if (userId == null) {
            throw new RuntimeException("请完成学号认证后查看本校商品");
        }

        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (isAdmin(user)) {
            return null;
        }
        return requireVerifiedSchool(user, "请完成学号认证后查看本校商品");
    }

    private void ensureProductVisible(HttpServletRequest request, Product product) {
        Long userId = sessionAuthSupport.currentUserId(request);
        if (userId == null) {
            throw new RuntimeException("请完成学号认证后查看本校商品");
        }

        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (isAdmin(user) || Objects.equals(userId, product.getSellerId())) {
            return;
        }

        requireVerifiedSeller(product.getSellerId());

        String userSchool = requireVerifiedSchool(user, "请完成学号认证后查看本校商品");
        String productSchool = trimToNull(product.getSchoolName());
        if (productSchool == null || !userSchool.equals(productSchool)) {
            throw new RuntimeException("该商品不属于当前学校，无法查看");
        }
        if (!"APPROVED".equals(product.getAuditStatus())
                || !"ON".equals(product.getStatus())
                || ordersRepository.existsByProductIdAndStatusNot(product.getId(), "CANCELLED")) {
            throw new RuntimeException("该商品暂不可查看");
        }
    }

    private SysUser requireVerifiedSellerWithSchool(Long userId) {
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        requireVerifiedSchool(user, "请先完成学号认证后再发布商品");
        return user;
    }

    private void requireVerifiedSeller(Long sellerId) {
        SysUser seller = sysUserRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("卖家不存在"));
        if (!"VERIFIED".equalsIgnoreCase(seller.getVerifyStatus())) {
            throw new RuntimeException("卖家认证状态已变更，该商品暂不可交易");
        }
    }

    private String requireVerifiedSchool(SysUser user, String message) {
        if (user == null
                || !"VERIFIED".equalsIgnoreCase(user.getVerifyStatus())
                || trimToNull(user.getSchool()) == null) {
            throw new RuntimeException(message);
        }
        return user.getSchool().trim();
    }

    private boolean isAdmin(SysUser user) {
        return user != null
                && ("ADMIN".equalsIgnoreCase(user.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(user.getRole()));
    }

    private Long parseLong(Object value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        return Long.parseLong(text);
    }

    private Product requireOwnedProduct(Long id, Long userId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        if (!Objects.equals(userId, product.getSellerId())) {
            throw new RuntimeException("无权限操作");
        }
        return product;
    }

    private Sort buildSort(String sortBy, String sortOrder) {
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
            return Sort.by(direction, sortBy);
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private ApiResp<?> buildPagedResp(int page, int size, Page<Product> productPage, List<Map<String, Object>> records) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", productPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        return ApiResp.ok(result);
    }
}
