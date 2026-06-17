package com.campus.twohand.user;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.user.service.AdminUserService;
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
@RequestMapping("/api/admin/user")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String keyword) {
        return adminUserService.page(request, page, size, keyword);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public ApiResp<?> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return adminUserService.create(request, body);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public ApiResp<?> update(HttpServletRequest request,
                             @PathVariable Long id,
                             @RequestBody Map<String, Object> body) {
        return adminUserService.update(request, id, body);
    }

    /**
     * 修改用户状态
     */
    @PutMapping("/{id}/status")
    public ApiResp<?> changeStatus(HttpServletRequest request,
                                   @PathVariable Long id,
                                   @RequestBody Map<String, Object> body) {
        return adminUserService.changeStatus(request, id, body);
    }

    /**
     * 批量修改用户状态
     */
    @PutMapping("/batch/status")
    public ApiResp<?> batchChangeStatus(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return adminUserService.batchChangeStatus(request, body);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ApiResp<?> delete(HttpServletRequest request, @PathVariable Long id) {
        return adminUserService.delete(request, id);
    }
}
