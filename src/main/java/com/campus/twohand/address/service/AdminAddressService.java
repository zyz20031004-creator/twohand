package com.campus.twohand.address.service;

import com.campus.twohand.address.repo.UserAddressRepository;
import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminAddressService {

    private final UserAddressRepository userAddressRepository;
    private final SessionAuthSupport sessionAuthSupport;

    public AdminAddressService(UserAddressRepository userAddressRepository, SessionAuthSupport sessionAuthSupport) {
        this.userAddressRepository = userAddressRepository;
        this.sessionAuthSupport = sessionAuthSupport;
    }

    public ApiResp<?> page(HttpServletRequest request, int page, int size, String keyword) {
        sessionAuthSupport.requireAdminId(request);

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;

        long total = userAddressRepository.adminCount(keyword);
        List<Map<String, Object>> records = userAddressRepository.adminPage(keyword, safeSize, offset);

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("records", records);
        return ApiResp.ok(data);
    }

    @Transactional
    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        sessionAuthSupport.requireAdminId(request);
        userAddressRepository.adminSoftDelete(id);
        return ApiResp.ok("ok");
    }

    @Transactional
    public ApiResp<?> batchDelete(HttpServletRequest request, Map<String, Object> body) {
        sessionAuthSupport.requireAdminId(request);

        Object idsObj = body.get("ids");
        if (!(idsObj instanceof List<?> list) || list.isEmpty()) {
            return ApiResp.ok("ok");
        }

        List<Long> ids = list.stream().map(item -> Long.valueOf(String.valueOf(item))).toList();
        userAddressRepository.adminSoftDeleteBatch(ids);
        return ApiResp.ok("ok");
    }
}
