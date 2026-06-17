package com.campus.twohand.product;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.product.service.AdminProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    /**
     * 商品管理列表分页查询
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String school,
                           @RequestParam(required = false) String auditStatus,
                           @RequestParam(required = false) String saleStatus) {
        return adminProductService.page(request, page, size, keyword, school, auditStatus, saleStatus);
    }

    /**
     * 商品详情查询
     */
    @GetMapping("/detail/{id}")
    public ApiResp<?> detail(HttpServletRequest request, @PathVariable Long id) {
        return adminProductService.detail(request, id);
    }

    /**
     * 审核通过商品
     */
    @PostMapping("/{id}/approve")
    public ApiResp<?> approve(HttpServletRequest request, @PathVariable Long id) {
        return adminProductService.approve(request, id);
    }

    /**
     * 驳回商品审核
     */
    @PostMapping("/{id}/reject")
    public ApiResp<?> reject(HttpServletRequest request, @PathVariable Long id, @RequestBody RejectReq req) {
        return adminProductService.reject(request, id, req == null ? null : req.getReason());
    }

    /**
     * 下架商品
     */
    @PostMapping("/{id}/off")
    public ApiResp<?> off(HttpServletRequest request, @PathVariable Long id, @RequestBody RejectReq req) {
        return adminProductService.off(request, id, req == null ? null : req.getReason());
    }

    /**
     * 上架商品
     */
    @PostMapping("/{id}/on")
    public ApiResp<?> on(HttpServletRequest request, @PathVariable Long id) {
        return adminProductService.on(request, id);
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public ApiResp<?> delete(HttpServletRequest request, @PathVariable Long id) {
        return adminProductService.delete(request, id);
    }

    /**
     * 批量删除商品
     */
    @PostMapping("/batch-delete")
    public ApiResp<?> batchDelete(HttpServletRequest request, @RequestBody BatchDeleteReq req) {
        return adminProductService.batchDelete(request, req == null ? null : req.getIds());
    }

    @Data
    public static class RejectReq {
        private String reason;
    }

    @Data
    public static class BatchDeleteReq {
        private List<Long> ids;
    }
}
