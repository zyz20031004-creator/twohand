package com.campus.twohand.notice.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.notice.repo.NoticeRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminNoticeService {

    private final NoticeRepository noticeRepository;
    private final SessionAuthSupport sessionAuthSupport;

    public AdminNoticeService(NoticeRepository noticeRepository, SessionAuthSupport sessionAuthSupport) {
        this.noticeRepository = noticeRepository;
        this.sessionAuthSupport = sessionAuthSupport;
    }

    public ApiResp<?> page(HttpServletRequest request,
                           int page,
                           int size,
                           String keyword,
                           Integer status) {
        sessionAuthSupport.requireAdminId(request);

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;

        long total = noticeRepository.adminCount(keyword, status);
        List<Map<String, Object>> records = noticeRepository.adminPage(keyword, status, safeSize, offset);

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("records", records);
        return ApiResp.ok(data);
    }

    @Transactional
    public ApiResp<?> create(HttpServletRequest request, Map<String, Object> body) {
        Long creatorId = sessionAuthSupport.requireAdminId(request);

        String title = String.valueOf(body.getOrDefault("title", "")).trim();
        String content = String.valueOf(body.getOrDefault("content", "")).trim();
        Integer status = body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status")));

        noticeRepository.adminInsert(title, content, creatorId, status);
        return ApiResp.ok("ok");
    }

    @Transactional
    public ApiResp<?> update(HttpServletRequest request, Long id, Map<String, Object> body) {
        sessionAuthSupport.requireAdminId(request);

        String title = String.valueOf(body.getOrDefault("title", "")).trim();
        String content = String.valueOf(body.getOrDefault("content", "")).trim();
        Integer status = body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status")));

        noticeRepository.adminUpdate(id, title, content, status);
        return ApiResp.ok("ok");
    }

    @Transactional
    public ApiResp<?> setStatus(HttpServletRequest request, Long id, Map<String, Object> body) {
        sessionAuthSupport.requireAdminId(request);
        Integer status = Integer.valueOf(String.valueOf(body.get("status")));
        noticeRepository.adminSetStatus(id, status);
        return ApiResp.ok("ok");
    }

    @Transactional
    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        sessionAuthSupport.requireAdminId(request);
        noticeRepository.adminDeleteById(id);
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
        noticeRepository.adminDeleteBatch(ids);
        return ApiResp.ok("ok");
    }
}
