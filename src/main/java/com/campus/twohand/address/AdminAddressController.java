package com.campus.twohand.address;

import com.campus.twohand.address.service.AdminAddressService;
import com.campus.twohand.common.ApiResp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/address")
public class AdminAddressController {

    private final AdminAddressService adminAddressService;

    public AdminAddressController(AdminAddressService adminAddressService) {
        this.adminAddressService = adminAddressService;
    }

    /**
     * 收货地址管理列表分页查询
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String keyword) {
        return adminAddressService.page(request, page, size, keyword);
    }

    /**
     * 删除收货地址
     */
    @DeleteMapping("/{id}")
    public ApiResp<?> delete(HttpServletRequest request, @PathVariable Long id) {
        return adminAddressService.delete(request, id);
    }

    /**
     * 批量删除收货地址
     */
    @DeleteMapping("/batch")
    public ApiResp<?> batchDelete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return adminAddressService.batchDelete(request, body);
    }
}
