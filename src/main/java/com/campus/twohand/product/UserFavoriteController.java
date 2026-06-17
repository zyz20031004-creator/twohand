package com.campus.twohand.product;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.product.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorite")
public class UserFavoriteController {

    private final FavoriteService favoriteService;

    public UserFavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * 我的收藏列表
     */
    @GetMapping("/myPage")
    public ApiResp<?> myPage(HttpServletRequest request,
                             @RequestParam int page,
                             @RequestParam int size,
                             @RequestParam(required = false) String keyword) {
        return favoriteService.myPage(request, page, size, keyword);
    }

    /**
     * 取消收藏
     */
    @PostMapping("/cancel")
    public ApiResp<?> cancel(HttpServletRequest request, @RequestParam Long productId) {
        return favoriteService.cancel(request, productId);
    }

    /**
     * 批量取消收藏
     */
    @PostMapping("/cancelBatch")
    public ApiResp<?> cancelBatch(HttpServletRequest request, @RequestBody List<Long> productIds) {
        return favoriteService.cancelBatch(request, productIds);
    }
}
