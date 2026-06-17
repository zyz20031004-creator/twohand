package com.campus.twohand.order;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.order.service.AdminOrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    /**
     * 订单管理列表分页查询
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String payType) {
        return adminOrderService.page(request, page, size, keyword, status, payType);
    }

    /**
     * 支付方式统计
     */
    @GetMapping("/payTypeStats")
    public ApiResp<?> payTypeStats(HttpServletRequest request,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String payType) {
        return adminOrderService.payTypeStats(request, keyword, status, payType);
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/{id}")
    public ApiResp<?> delete(HttpServletRequest request, @PathVariable Long id) {
        return adminOrderService.delete(request, id);
    }

    /**
     * 批量删除订单
     */
    @PostMapping("/batch-delete")
    public ApiResp<?> batchDelete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return adminOrderService.batchDelete(request, body);
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public ApiResp<?> cancel(HttpServletRequest request, @PathVariable Long id) {
        return adminOrderService.cancel(request, id);
    }
}
