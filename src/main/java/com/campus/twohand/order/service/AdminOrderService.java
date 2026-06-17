package com.campus.twohand.order.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.order.entity.Orders;
import com.campus.twohand.order.repo.OrdersRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminOrderService {

    private final OrdersRepository ordersRepository;
    private final SessionAuthSupport sessionAuthSupport;
    private final OrderService orderService;

    public AdminOrderService(OrdersRepository ordersRepository,
                             SessionAuthSupport sessionAuthSupport,
                             OrderService orderService) {
        this.ordersRepository = ordersRepository;
        this.sessionAuthSupport = sessionAuthSupport;
        this.orderService = orderService;
    }

    public ApiResp<?> page(HttpServletRequest request,
                           int page,
                           int size,
                           String keyword,
                           String status,
                           String payType) {
        sessionAuthSupport.requireAdminId(request);

        var pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var paged = ordersRepository.adminPageWithStatus(keyword, status, payType, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("total", paged.getTotalElements());
        data.put("records", paged.getContent());
        return ApiResp.ok(data);
    }

    public ApiResp<?> payTypeStats(HttpServletRequest request,
                                   String keyword,
                                   String status,
                                   String payType) {
        sessionAuthSupport.requireAdminId(request);

        List<Map<String, Object>> rows = ordersRepository.adminPayTypeStats(keyword, status, payType);
        Map<String, Object> data = new HashMap<>();
        data.put("stats", rows);
        return ApiResp.ok(data);
    }

    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        sessionAuthSupport.requireAdminId(request);

        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null) {
            return ApiResp.fail("订单不存在");
        }
        if (!canDelete(order)) {
            return ApiResp.fail("仅已完成或已取消订单可删除");
        }

        ordersRepository.deleteById(id);
        return ApiResp.ok(null);
    }

    public ApiResp<?> batchDelete(HttpServletRequest request, Map<String, Object> body) {
        sessionAuthSupport.requireAdminId(request);

        Object idsObj = body.get("ids");
        if (!(idsObj instanceof List<?> list) || list.isEmpty()) {
            return ApiResp.fail("ids不能为空");
        }
        List<Long> ids = list.stream().map(item -> Long.valueOf(String.valueOf(item))).toList();
        List<Orders> orders = ordersRepository.findAllById(ids);
        if (orders.size() != ids.size()) {
            return ApiResp.fail("存在无效订单");
        }
        if (orders.stream().anyMatch(order -> !canDelete(order))) {
            return ApiResp.fail("批量删除仅允许已完成或已取消订单");
        }

        ordersRepository.deleteByIds(ids);
        return ApiResp.ok(null);
    }

    @Transactional
    public ApiResp<?> cancel(HttpServletRequest request, Long id) {
        sessionAuthSupport.requireAdminId(request);

        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null) {
            return ApiResp.fail("订单不存在");
        }
        if (!"UNPAID".equals(order.getStatus())) {
            return ApiResp.fail("仅未支付订单可取消");
        }

        ordersRepository.updateStatus(id, "CANCELLED");
        orderService.restoreProductIfNeeded(order.getProductId());
        return ApiResp.ok(null);
    }

    private boolean canDelete(Orders order) {
        return "FINISHED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus());
    }
}