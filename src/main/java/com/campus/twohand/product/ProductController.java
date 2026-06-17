package com.campus.twohand.product;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.product.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 商品列表分页查询
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String auditStatus,
                           @RequestParam(required = false) String sortBy,
                           @RequestParam(required = false) String sortOrder) {
        return productService.page(request, page, size, keyword, categoryId, status, auditStatus, sortBy, sortOrder);
    }

    /**
     * 商品详情查询
     */
    @GetMapping("/detail/{id}")
    public ApiResp<?> detail(HttpServletRequest request,
                             @PathVariable Long id,
                             @RequestParam(required = false) Long userId) {
        return productService.detail(request, id, userId);
    }

    /**
     * 商品收藏切换
     */
    @PostMapping("/favorite/toggle")
    public ApiResp<?> toggleFavorite(HttpServletRequest request, @RequestParam Long productId) {
        return productService.toggleFavorite(request, productId);
    }

    /**
     * 商品点赞切换
     */
    @PostMapping("/like/toggle")
    public ApiResp<?> toggleLike(HttpServletRequest request, @RequestParam Long productId) {
        return productService.toggleLike(request, productId);
    }

    /**
     * 我的商品列表
     */
    @GetMapping("/my/page")
    public ApiResp<?> myPage(HttpServletRequest request,
                             @RequestParam int page,
                             @RequestParam int size,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status) {
        return productService.myPage(request, page, size, keyword, status);
    }

    /**
     * 商品上下架切换
     */
    @PutMapping("/{id}/toggle")
    public ApiResp<?> toggle(HttpServletRequest request, @PathVariable Long id) {
        return productService.toggle(request, id);
    }

    /**
     * 更新商品信息
     */
    @PutMapping("/{id}")
    public ApiResp<?> update(HttpServletRequest request,
                             @PathVariable Long id,
                             @RequestBody Map<String, Object> req) {
        return productService.update(request, id, req);
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public ApiResp<?> delete(HttpServletRequest request, @PathVariable Long id) {
        return productService.delete(request, id);
    }

    /**
     * 创建商品
     */
    @PostMapping
    public ApiResp<?> create(HttpServletRequest request, @RequestBody Map<String, Object> req) {
        return productService.create(request, req);
    }
}
