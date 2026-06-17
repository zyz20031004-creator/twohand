package com.campus.twohand.wanted.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.wanted.repo.WantedRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminWantedService {

    private final WantedRepository wantedRepository;
    private final SessionAuthSupport sessionAuthSupport;

    public AdminWantedService(WantedRepository wantedRepository, SessionAuthSupport sessionAuthSupport) {
        this.wantedRepository = wantedRepository;
        this.sessionAuthSupport = sessionAuthSupport;
    }

    public ApiResp<?> page(HttpServletRequest request,
                           int page,
                           int size,
                           String keyword,
                           String status) {
        sessionAuthSupport.requireAdminId(request);

        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        int offset = (p - 1) * s;
        String safeKeyword = trimToNull(keyword);
        String safeStatus = trimToNull(status);

        long total = wantedRepository.adminCount(safeKeyword, safeStatus);
        List<Object[]> rows = wantedRepository.adminPage(safeKeyword, safeStatus, s, offset);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[0]);
            item.put("userId", row[1]);
            item.put("username", row[2]);
            item.put("title", row[3]);
            item.put("content", row[4]);
            item.put("imageUrl", row[5]);
            item.put("status", row[6]);
            item.put("viewCount", row[7]);
            item.put("createdAt", row[8]);
            item.put("schoolName", row.length > 9 ? row[9] : null);
            records.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("records", records);
        return ApiResp.ok(data);
    }

    @Transactional
    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        sessionAuthSupport.requireAdminId(request);
        wantedRepository.adminDeleteById(id);
        return ApiResp.ok("ok");
    }

    @Transactional
    public ApiResp<?> batchDelete(HttpServletRequest request, Map<String, Object> body) {
        sessionAuthSupport.requireAdminId(request);

        Object idsObj = body == null ? null : body.get("ids");
        if (!(idsObj instanceof List<?> list) || list.isEmpty()) {
            return ApiResp.ok("ok");
        }

        List<Long> ids = list.stream().map(item -> Long.valueOf(String.valueOf(item))).toList();
        wantedRepository.adminDeleteBatch(ids);
        return ApiResp.ok("ok");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
