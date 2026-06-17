package com.campus.twohand.comment;

import com.campus.twohand.comment.service.CommentService;
import com.campus.twohand.common.ApiResp;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 商品评论列表
     */
    @GetMapping("/product/list")
    public ApiResp<?> list(@RequestParam Long productId) {
        return commentService.listProduct(productId);
    }

    /**
     * 添加商品评论
     */
    @PostMapping("/product/add")
    public ApiResp<?> addProduct(HttpServletRequest request, @RequestBody AddCommentReq req) {
        return commentService.addProduct(request, req.getProductId(), req.getParentId(), req.getContent());
    }

    /**
     * 商品评论分页
     */
    @GetMapping("/product/page")
    public ApiResp<?> productPage(HttpServletRequest request,
                                  @RequestParam Long productId,
                                  @RequestParam int page,
                                  @RequestParam int size,
                                  @RequestParam(required = false) Long currentUserId) {
        return commentService.productPage(request, productId, page, size, currentUserId);
    }

    /**
     * 删除商品评论
     */
    @DeleteMapping("/product/delete/{id}")
    public ApiResp<?> deleteProductComment(HttpServletRequest request,
                                           @PathVariable Long id,
                                           @RequestParam(required = false) Long userId) {
        return commentService.deleteProductComment(request, id);
    }

    /**
     * 评论点赞切换
     */
    @PostMapping("/product/like/toggle")
    public ApiResp<?> toggleLike(HttpServletRequest request,
                                 @RequestParam Long productId,
                                 @RequestParam(required = false) Long userId) {
        return commentService.toggleLike(request, productId);
    }

    /**
     * 商品评论树
     */
    @GetMapping("/product/tree")
    public ApiResp<?> productTree(HttpServletRequest request,
                                  @RequestParam Long productId,
                                  @RequestParam(required = false) Long currentUserId) {
        return commentService.productTree(request, productId, currentUserId);
    }

    /**
     * 求购评论列表
     */
    @GetMapping("/wanted/list")
    public ApiResp<?> wantedList(@RequestParam Long targetId) {
        return commentService.wantedList(targetId);
    }

    /**
     * 求购评论树
     */
    @GetMapping("/wanted/tree")
    public ApiResp<?> wantedTree(HttpServletRequest request,
                                 @RequestParam Long targetId,
                                 @RequestParam(required = false) Long currentUserId) {
        return commentService.wantedTree(request, targetId, currentUserId);
    }

    /**
     * 添加求购评论
     */
    @PostMapping("/wanted/add")
    public ApiResp<?> addWanted(HttpServletRequest request, @RequestBody AddWantedCommentReq req) {
        return commentService.addWanted(request, req.getTargetId(), req.getParentId(), req.getContent());
    }

    /**
     * 删除求购评论
     */
    @DeleteMapping("/wanted/delete/{id}")
    public ApiResp<?> deleteWantedComment(HttpServletRequest request,
                                          @PathVariable Long id,
                                          @RequestParam(required = false) Long userId) {
        return commentService.deleteWantedComment(request, id);
    }

    @Data
    public static class AddCommentReq {
        private Long productId;
        private Long userId;
        private Long parentId;
        private String content;
    }

    @Data
    public static class AddWantedCommentReq {
        private Long targetId;
        private Long userId;
        private Long parentId;
        private String content;
    }

    @PostConstruct
    public void init() {
        System.out.println("CommentController loaded");
    }
}
