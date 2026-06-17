package com.campus.twohand.user;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.user.service.AdminAdminService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/admin/admin")
public class AdminAdminController {

    private final AdminAdminService adminAdminService;

    public AdminAdminController(AdminAdminService adminAdminService) {
        this.adminAdminService = adminAdminService;
    }

    /**
     * 分页查询管理员列表
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String keyword) {
        return adminAdminService.page(request, page, size, keyword);
    }

    /**
     * 创建管理员
     */
    @PostMapping
    public ApiResp<?> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return adminAdminService.create(request, body);
    }

    /**
     * 更新管理员信息
     */
    @PutMapping("/{id}")
    public ApiResp<?> update(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return adminAdminService.update(request, id, body);
    }

    /**
     * 删除管理员
     */
    @DeleteMapping("/{id}")
    public ApiResp<?> delete(HttpServletRequest request, @PathVariable Long id) {
        return adminAdminService.delete(request, id);
    }

    /**
     * 批量删除管理员
     */
    @DeleteMapping("/batch")
    public ApiResp<?> batchDelete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return adminAdminService.batchDelete(request, body);
    }
}
