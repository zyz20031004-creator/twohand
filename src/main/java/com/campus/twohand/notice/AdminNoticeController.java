package com.campus.twohand.notice;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.notice.service.AdminNoticeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/notice")
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    public AdminNoticeController(AdminNoticeService adminNoticeService) {
        this.adminNoticeService = adminNoticeService;
    }

    /**
     * 公告管理列表分页查询
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Integer status) {
        return adminNoticeService.page(request, page, size, keyword, status);
    }

    /**
     * 创建公告
     */
    @PostMapping
    public ApiResp<?> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return adminNoticeService.create(request, body);
    }

    /**
     * 更新公告
     */
    @PutMapping("/{id}")
    public ApiResp<?> update(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return adminNoticeService.update(request, id, body);
    }

    /**
     * 设置公告状态
     */
    @PutMapping("/{id}/status")
    public ApiResp<?> setStatus(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return adminNoticeService.setStatus(request, id, body);
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    public ApiResp<?> delete(HttpServletRequest request, @PathVariable Long id) {
        return adminNoticeService.delete(request, id);
    }

    /**
     * 批量删除公告
     */
    @DeleteMapping("/batch")
    public ApiResp<?> batchDelete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return adminNoticeService.batchDelete(request, body);
    }
}
