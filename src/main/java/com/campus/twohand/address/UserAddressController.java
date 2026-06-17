package com.campus.twohand.address;

import com.campus.twohand.address.service.UserAddressService;
import com.campus.twohand.common.ApiResp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/address")
public class UserAddressController {

    private final UserAddressService userAddressService;

    public UserAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    /**
     * 获取我的收货地址列表
     */
    @GetMapping("/my/list")
    public ApiResp<?> myList(HttpServletRequest request) {
        return userAddressService.myList(request);
    }

    /**
     * 创建收货地址
     */
    @PostMapping("/create")
    public ApiResp<?> create(HttpServletRequest request, @RequestBody SaveReq req) {
        return userAddressService.create(request, req);
    }

    /**
     * 更新收货地址
     */
    @PutMapping("/update/{id}")
    public ApiResp<?> update(HttpServletRequest request, @PathVariable Long id, @RequestBody SaveReq req) {
        return userAddressService.update(request, id, req);
    }

    /**
     * 删除收货地址
     */
    @PostMapping("/delete/{id}")
    public ApiResp<?> delete(HttpServletRequest request, @PathVariable Long id) {
        return userAddressService.delete(request, id);
    }

    /**
     * 设置默认收货地址
     */
    @PostMapping("/default/{id}")
    public ApiResp<?> setDefault(HttpServletRequest request, @PathVariable Long id) {
        return userAddressService.setDefault(request, id);
    }

    public static class SaveReq {
        public String contactName;
        public String contactPhone;
        public String addressText;
        public Integer isDefault;
    }
}
