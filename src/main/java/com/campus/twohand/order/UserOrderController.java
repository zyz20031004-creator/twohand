package com.campus.twohand.order;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class UserOrderController {

    private final OrderService orderService;

    public UserOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 我的订单列表
     */
    @GetMapping("/myPage")
    public ApiResp<?> myPage(HttpServletRequest request,
                             @RequestParam String type,
                             @RequestParam int page,
                             @RequestParam int size,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String payType) {
        return orderService.myPage(request, type, page, size, keyword, status, payType);
    }

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public ApiResp<?> create(HttpServletRequest request,
                             @RequestParam Long productId,
                             @RequestParam(required = false) Long addressId) {
        return orderService.create(request, productId, addressId);
    }

    /**
     * 订单支付
     */
    @PostMapping("/pay")
    public ApiResp<?> pay(HttpServletRequest request,
                          @RequestParam Long id,
                          @RequestParam(defaultValue = "WECHAT") String payType) {
        return orderService.pay(request, id, payType);
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    public ApiResp<?> cancel(HttpServletRequest request, @RequestParam Long id) {
        return orderService.cancel(request, id);
    }

    /**
     * 确认收货
     */
    @PostMapping("/finish")
    public ApiResp<?> finish(HttpServletRequest request,
                             @RequestParam Long id,
                             @RequestParam(required = false) String review) {
        return orderService.finish(request, id, review);
    }

    /**
     * 隐藏/删除订单
     */
    @PostMapping({"/hide", "/delete"})
    public ApiResp<?> hide(HttpServletRequest request, @RequestParam Long id) {
        return orderService.hide(request, id);
    }
}


