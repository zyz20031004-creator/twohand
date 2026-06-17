package com.campus.twohand.product.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.product.entity.Favorite;
import com.campus.twohand.product.repo.FavoriteRepository;
import com.campus.twohand.product.support.ProductImageSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository repo;
    private final SessionAuthSupport sessionAuthSupport;

    public ApiResp<?> myPage(HttpServletRequest request, int page, int size, String keyword) {
        Long uid = sessionAuthSupport.requireUserId(request);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1));
        Page<Map<String, Object>> paged = repo.userFavPage(uid, keyword, pageable);

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
    public ApiResp<?> cancel(HttpServletRequest request, Long productId) {
        Long uid = sessionAuthSupport.requireUserId(request);
        Favorite favorite = repo.findByUserIdAndProductId(uid, productId)
                .orElseThrow(() -> new RuntimeException("收藏不存在"));
        favorite.setStatus(0);
        repo.save(favorite);
        return ApiResp.ok(null);
    }

    @Transactional
    public ApiResp<?> cancelBatch(HttpServletRequest request, List<Long> productIds) {
        Long uid = sessionAuthSupport.requireUserId(request);
        if (productIds == null || productIds.isEmpty()) {
            throw new RuntimeException("请选择要删除的收藏");
        }
        repo.batchCancel(uid, productIds);
        return ApiResp.ok(null);
    }
}

