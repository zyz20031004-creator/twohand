package com.campus.twohand.order;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.order.service.AdminDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/stats")
    public ApiResp<?> stats(HttpServletRequest request) {
        return adminDashboardService.stats(request);
    }
}
