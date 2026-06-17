package com.campus.twohand.feedback.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.feedback.entity.Feedback;
import com.campus.twohand.feedback.repo.FeedbackRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminFeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final SessionAuthSupport sessionAuthSupport;

    public ApiResp<?> pageLegacy(HttpServletRequest request, int page, int size) {
        sessionAuthSupport.requireAdminId(request);

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1));
        Page<Feedback> paged = feedbackRepository.findAllByOrderByCreatedAtDesc(pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("total", paged.getTotalElements());
        data.put("records", paged.getContent());
        return ApiResp.ok(data);
    }

    @Transactional
    public ApiResp<?> replyLegacy(HttpServletRequest request, Map<String, Object> body) {
        Long adminId = sessionAuthSupport.requireAdminId(request);

        Long id = parseLong(value(body, "id"));
        String reply = str(body, "reply");
        String close = str(body, "close");

        if (id == null) {
            return ApiResp.fail("缺少id");
        }
        if (!StringUtils.hasText(reply)) {
            return ApiResp.fail("请输入回复内容");
        }

        Feedback feedback = feedbackRepository.findById(id).orElse(null);
        if (feedback == null) {
            return ApiResp.fail("反馈不存在");
        }

        feedback.setReply(reply);
        feedback.setRepliedBy(adminId);
        feedback.setRepliedAt(LocalDateTime.now());
        if ("1".equals(close)) {
            feedback.setStatus("CLOSED");
        }

        feedbackRepository.save(feedback);
        return ApiResp.ok(true);
    }

    @Transactional
    public ApiResp<?> closeLegacy(HttpServletRequest request, Map<String, Object> body) {
        sessionAuthSupport.requireAdminId(request);

        Long id = parseLong(value(body, "id"));
        if (id == null) {
            return ApiResp.fail("缺少id");
        }

        Feedback feedback = feedbackRepository.findById(id).orElse(null);
        if (feedback == null) {
            return ApiResp.fail("反馈不存在");
        }

        feedback.setStatus("CLOSED");
        feedbackRepository.save(feedback);
        return ApiResp.ok(true);
    }

    public ApiResp<?> page(HttpServletRequest request, int page, int size, String keyword) {
        sessionAuthSupport.requireAdminId(request);

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;

        String safeKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        long total = feedbackRepository.adminCount(safeKeyword);
        List<Map<String, Object>> records = feedbackRepository.adminPage(safeKeyword, safeSize, offset);

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("records", records);
        return ApiResp.ok(data);
    }

    @Transactional
    public ApiResp<?> reply(HttpServletRequest request, Long id, Map<String, Object> body) {
        sessionAuthSupport.requireAdminId(request);

        String reply = str(body, "reply");
        if (reply.isEmpty()) {
            return ApiResp.fail("reply不能为空");
        }

        Long repliedBy = 0L;
        Long bodyRepliedBy = parseLong(value(body, "repliedBy"));
        if (bodyRepliedBy != null) {
            repliedBy = bodyRepliedBy;
        }

        feedbackRepository.adminReply(id, reply, repliedBy);
        return ApiResp.ok("ok");
    }

    @Transactional
    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        sessionAuthSupport.requireAdminId(request);
        feedbackRepository.deleteById(id);
        return ApiResp.ok("ok");
    }

    @Transactional
    public ApiResp<?> batchDelete(HttpServletRequest request, Map<String, Object> body) {
        sessionAuthSupport.requireAdminId(request);

        Object idsObj = value(body, "ids");
        if (!(idsObj instanceof List<?> list) || list.isEmpty()) {
            return ApiResp.ok("ok");
        }

        List<Long> ids = list.stream().map(item -> Long.valueOf(String.valueOf(item))).toList();
        feedbackRepository.adminDeleteBatch(ids);
        return ApiResp.ok("ok");
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Object value(Map<String, Object> body, String key) {
        return body == null ? null : body.get(key);
    }

    private String str(Map<String, Object> body, String key) {
        Object value = value(body, key);
        return value == null ? "" : String.valueOf(value).trim();
    }
}
