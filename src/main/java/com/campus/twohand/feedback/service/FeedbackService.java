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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final SessionAuthSupport sessionAuthSupport;

    @Transactional
    public ApiResp<?> submit(HttpServletRequest request, Map<String, Object> body) {
        String subject = str(body, "subject");
        String content = str(body, "content");
        String contact = str(body, "contact");
        String email = str(body, "email");

        if (!StringUtils.hasText(subject)) {
            return ApiResp.fail("请输入标题");
        }
        if (!StringUtils.hasText(content)) {
            return ApiResp.fail("请输入内容");
        }
        if (!StringUtils.hasText(contact)) {
            return ApiResp.fail("请输入联系方式");
        }

        Feedback feedback = new Feedback();
        feedback.setUserId(sessionAuthSupport.currentUserId(request));
        feedback.setSubject(subject);
        feedback.setContent(content);
        feedback.setContact(contact);
        feedback.setEmail(StringUtils.hasText(email) ? email : null);
        feedback.setStatus("OPEN");
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);

        Map<String, Object> data = new HashMap<>();
        data.put("id", feedback.getId());
        data.put("anonymous", feedback.getUserId() == null);
        return ApiResp.ok(data);
    }

    public ApiResp<?> myPage(HttpServletRequest request,
                             int page,
                             int size,
                             String status,
                             String keyword) {
        Long currentUserId = sessionAuthSupport.requireUserId(request);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1));
        String safeStatus = StringUtils.hasText(status) ? status.trim() : null;
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<Object[]> paged = feedbackRepository.pageMyFeedback(currentUserId, safeStatus, kw, pageable);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : paged.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[0]);
            item.put("subject", row[1]);
            item.put("content", row[2]);
            item.put("contact", row[3]);
            item.put("email", row[4]);
            item.put("status", row[5]);
            item.put("reply", row[6]);
            item.put("createdAt", row[7]);
            list.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", paged.getTotalElements());
        data.put("records", list);
        return ApiResp.ok(data);
    }

    @Transactional
    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        Long currentUserId = sessionAuthSupport.requireUserId(request);
        Feedback feedback = feedbackRepository.findById(id).orElse(null);
        if (feedback == null) {
            return ApiResp.fail("反馈不存在");
        }
        if (feedback.getUserId() == null) {
            return ApiResp.fail("匿名反馈不支持在个人中心删除");
        }
        if (!currentUserId.equals(feedback.getUserId())) {
            return ApiResp.fail("无权操作");
        }

        feedbackRepository.delete(feedback);
        return ApiResp.ok(true);
    }

    private String str(Map<String, Object> body, String key) {
        if (body == null) {
            return "";
        }
        Object value = body.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }
}
