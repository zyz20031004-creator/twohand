package com.campus.twohand.category;

import com.campus.twohand.category.service.AdminCategoryService;
import com.campus.twohand.common.ApiResp;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    /**
     * 分类管理列表分页查询
     */
    @GetMapping
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Integer status) {
        return adminCategoryService.page(request, page, size, keyword, status);
    }

    /**
     * 创建分类
     */
    @PostMapping
    public ApiResp<?> create(HttpServletRequest request, @RequestBody SaveReq req) {
        return adminCategoryService.create(request, req);
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public ApiResp<?> update(HttpServletRequest request, @PathVariable Long id, @RequestBody SaveReq req) {
        return adminCategoryService.update(request, id, req);
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public ApiResp<?> delete(HttpServletRequest request, @PathVariable Long id) {
        return adminCategoryService.delete(request, id);
    }

    /**
     * 批量删除分类
     */
    @PostMapping("/batch-delete")
    public ApiResp<?> batchDelete(HttpServletRequest request, @RequestBody BatchDeleteReq req) {
        return adminCategoryService.batchDelete(request, req);
    }

    @Data
    public static class SaveReq {
        private String name;
    }

    @Data
    public static class BatchDeleteReq {
        private List<Long> ids;
    }
}
