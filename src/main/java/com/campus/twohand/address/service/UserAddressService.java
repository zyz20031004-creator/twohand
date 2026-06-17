package com.campus.twohand.address.service;

import com.campus.twohand.address.UserAddressController;
import com.campus.twohand.address.entity.UserAddress;
import com.campus.twohand.address.repo.UserAddressRepository;
import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserAddressService {

    private final UserAddressRepository repo;
    private final SessionAuthSupport sessionAuthSupport;

    public UserAddressService(UserAddressRepository repo, SessionAuthSupport sessionAuthSupport) {
        this.repo = repo;
        this.sessionAuthSupport = sessionAuthSupport;
    }

    public ApiResp<?> myList(HttpServletRequest request) {
        Long uid = sessionAuthSupport.requireUserId(request);
        List<UserAddress> list = repo.findByUserIdAndStatusOrderByIsDefaultDescIdDesc(uid, 1);
        return ApiResp.ok(list);
    }

    @Transactional
    public ApiResp<?> create(HttpServletRequest request, UserAddressController.SaveReq req) {
        Long uid = sessionAuthSupport.requireUserId(request);
        Integer isDefault = req.isDefault == null ? 0 : req.isDefault;
        if (isDefault == 1) {
            repo.clearDefault(uid);
        }

        UserAddress address = new UserAddress();
        address.setUserId(uid);
        address.setContactName(req.contactName);
        address.setContactPhone(req.contactPhone);
        address.setAddressText(req.addressText);
        address.setIsDefault(isDefault);
        address.setStatus(1);
        LocalDateTime now = LocalDateTime.now();
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        repo.save(address);
        return ApiResp.ok(null);
    }

    @Transactional
    public ApiResp<?> update(HttpServletRequest request, Long id, UserAddressController.SaveReq req) {
        Long uid = sessionAuthSupport.requireUserId(request);
        UserAddress address = repo.findById(id).orElseThrow(() -> new RuntimeException("地址不存在"));
        if (!uid.equals(address.getUserId())) {
            throw new RuntimeException("无权限操作");
        }

        Integer isDefault = req.isDefault == null ? 0 : req.isDefault;
        if (isDefault == 1) {
            repo.clearDefault(uid);
        }

        address.setContactName(req.contactName);
        address.setContactPhone(req.contactPhone);
        address.setAddressText(req.addressText);
        address.setIsDefault(isDefault);
        address.setUpdatedAt(LocalDateTime.now());
        repo.save(address);
        return ApiResp.ok(null);
    }

    @Transactional
    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        Long uid = sessionAuthSupport.requireUserId(request);
        UserAddress address = repo.findById(id).orElseThrow(() -> new RuntimeException("地址不存在"));
        if (!uid.equals(address.getUserId())) {
            throw new RuntimeException("无权限操作");
        }
        address.setStatus(0);
        address.setUpdatedAt(LocalDateTime.now());
        repo.save(address);
        return ApiResp.ok(null);
    }

    @Transactional
    public ApiResp<?> setDefault(HttpServletRequest request, Long id) {
        Long uid = sessionAuthSupport.requireUserId(request);
        UserAddress address = repo.findById(id).orElseThrow(() -> new RuntimeException("地址不存在"));
        if (!uid.equals(address.getUserId())) {
            throw new RuntimeException("无权限操作");
        }
        repo.clearDefault(uid);
        address.setIsDefault(1);
        address.setUpdatedAt(LocalDateTime.now());
        repo.save(address);
        return ApiResp.ok(null);
    }
}
