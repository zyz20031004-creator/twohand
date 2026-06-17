package com.campus.twohand.notice.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.notice.entity.Notice;
import com.campus.twohand.notice.repo.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public ApiResp<?> page(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1));
        Page<Notice> paged = noticeRepository.pageOnline(pageable);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Notice notice : paged.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", notice.getId());
            item.put("title", notice.getTitle());
            item.put("createdAt", notice.getCreatedAt());
            records.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", paged.getTotalElements());
        data.put("records", records);
        return ApiResp.ok(data);
    }

    public ApiResp<?> detail(Long id) {
        Notice notice = noticeRepository.findById(id).orElse(null);
        if (notice == null || notice.getStatus() == null || notice.getStatus() != 1) {
            return ApiResp.fail("公告不存在或已下线");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", notice.getId());
        data.put("title", notice.getTitle());
        data.put("content", notice.getContent());
        data.put("createdAt", notice.getCreatedAt());
        return ApiResp.ok(data);
    }
}
