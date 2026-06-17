package com.campus.twohand.wanted;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.wanted.service.AdminWantedService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/wanted")
public class AdminWantedController {

    private final AdminWantedService adminWantedService;

    public AdminWantedController(AdminWantedService adminWantedService) {
        this.adminWantedService = adminWantedService;
    }

    /**
     * 求购管理列表分页查询
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        return adminWantedService.page(request, page, size, keyword, status);
    }

    /**
     * 删除求购信息
     */
    @DeleteMapping("/{id}")
    public ApiResp<?> delete(HttpServletRequest request, @PathVariable Long id) {
        return adminWantedService.delete(request, id);
    }

    /**
     * 批量删除求购信息
     */
    @DeleteMapping("/batch")
    public ApiResp<?> batchDelete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return adminWantedService.batchDelete(request, body);
    }
}
