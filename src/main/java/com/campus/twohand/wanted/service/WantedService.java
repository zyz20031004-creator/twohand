package com.campus.twohand.wanted.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.SysUserRepository;
import com.campus.twohand.verify.service.VerifyGuard;
import com.campus.twohand.wanted.entity.Wanted;
import com.campus.twohand.wanted.repo.WantedRepository;
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
public class WantedService {

    private final WantedRepository wantedRepository;
    private final VerifyGuard verifyGuard;
    private final SessionAuthSupport sessionAuthSupport;
    private final SysUserRepository sysUserRepository;

    public ApiResp<?> page(HttpServletRequest request, int page, int size, String status, String keyword) {
        Long userId = sessionAuthSupport.requireUserId(request);
        String schoolName = requireVerifiedSchool(userId);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1));
        String safeStatus = StringUtils.hasText(status) ? status.trim() : null;
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<Object[]> paged = wantedRepository.pageWithUser(safeStatus, kw, schoolName, pageable);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : paged.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[0]);
            item.put("userId", row[1]);
            item.put("title", row[2]);
            item.put("content", row[3]);
            item.put("imageUrl", row[4]);
            item.put("status", row[5]);
            item.put("viewCount", row[6]);
            item.put("createdAt", row[7]);
            item.put("nickname", row[8]);
            item.put("avatar", row[9]);
            item.put("avatarUrl", row[9]);
            item.put("commentCount", row[10]);
            item.put("schoolName", row[11]);
            list.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", paged.getTotalElements());
        data.put("records", list);
        return ApiResp.ok(data);
    }

    public ApiResp<?> myPage(HttpServletRequest request,
                             int page,
                             int size,
                             String status,
                             String keyword) {
        Long userId = sessionAuthSupport.requireUserId(request);

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1));
        String safeStatus = StringUtils.hasText(status) ? status.trim() : null;
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<Object[]> paged = wantedRepository.pageMyWanted(userId, safeStatus, kw, pageable);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : paged.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[0]);
            item.put("title", row[1]);
            item.put("content", row[2]);
            item.put("imageUrl", row[3]);
            item.put("status", row[4]);
            item.put("createdAt", row[5]);
            item.put("schoolName", row.length > 6 ? row[6] : null);
            item.put("viewCount", row.length > 7 ? row[7] : 0);
            list.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", paged.getTotalElements());
        data.put("records", list);
        return ApiResp.ok(data);
    }

    @Transactional
    public ApiResp<?> create(HttpServletRequest request, Map<String, Object> body) {
        Long userId = sessionAuthSupport.requireUserId(request);

        String title = str(body, "title");
        String content = str(body, "content");
        String imageUrl = str(body, "imageUrl");

        if (!StringUtils.hasText(title)) {
            return ApiResp.fail("title is required");
        }
        if (!StringUtils.hasText(content)) {
            return ApiResp.fail("content is required");
        }

        verifyGuard.requireVerified(userId);
        String schoolName = requireVerifiedSchool(userId);

        Wanted wanted = new Wanted();
        wanted.setTitle(title);
        wanted.setContent(content);
        wanted.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl : null);
        wanted.setUserId(userId);
        wanted.setSchoolName(schoolName);
        wanted.setStatus("OPEN");
        wanted.setViewCount(0);
        LocalDateTime now = LocalDateTime.now();
        wanted.setCreatedAt(now);
        wanted.setUpdatedAt(now);

        wantedRepository.save(wanted);

        Map<String, Object> data = new HashMap<>();
        data.put("id", wanted.getId());
        return ApiResp.ok(data);
    }

    @Transactional
    public ApiResp<?> update(HttpServletRequest request, Long id, Map<String, Object> body) {
        Long userId = sessionAuthSupport.requireUserId(request);

        String title = str(body, "title");
        String content = str(body, "content");
        String imageUrl = str(body, "imageUrl");

        if (!StringUtils.hasText(title)) {
            return ApiResp.fail("title is required");
        }
        if (!StringUtils.hasText(content)) {
            return ApiResp.fail("content is required");
        }

        Wanted wanted = wantedRepository.findById(id).orElse(null);
        if (wanted == null) {
            return ApiResp.fail("wanted not found");
        }
        if (!userId.equals(wanted.getUserId())) {
            return ApiResp.fail("no permission");
        }

        wanted.setTitle(title);
        wanted.setContent(content);
        wanted.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl : null);
        wanted.setUpdatedAt(LocalDateTime.now());
        wantedRepository.save(wanted);
        return ApiResp.ok(true);
    }

    @Transactional
    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        Long userId = sessionAuthSupport.requireUserId(request);

        Wanted wanted = wantedRepository.findById(id).orElse(null);
        if (wanted == null) {
            return ApiResp.fail("wanted not found");
        }
        if (!userId.equals(wanted.getUserId())) {
            return ApiResp.fail("no permission");
        }

        wantedRepository.delete(wanted);
        return ApiResp.ok(true);
    }

    @Transactional
    public ApiResp<?> solve(HttpServletRequest request, Long id) {
        Long userId = sessionAuthSupport.requireUserId(request);

        Wanted wanted = wantedRepository.findById(id).orElse(null);
        if (wanted == null) {
            return ApiResp.fail("wanted not found");
        }
        if (!userId.equals(wanted.getUserId())) {
            return ApiResp.fail("no permission");
        }

        wanted.setStatus("SOLVED");
        wanted.setUpdatedAt(LocalDateTime.now());
        wantedRepository.save(wanted);
        return ApiResp.ok(true);
    }

    @Transactional
    public ApiResp<?> increaseViewCount(Long id) {
        if (id == null || !wantedRepository.existsById(id)) {
            return ApiResp.fail("wanted not found");
        }
        wantedRepository.increaseViewCount(id);
        return ApiResp.ok(true);
    }

    private String str(Map<String, Object> body, String key) {
        if (body == null) {
            return "";
        }
        Object value = body.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String requireVerifiedSchool(Long userId) {
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String school = user.getSchool() == null ? "" : user.getSchool().trim();
        if (!"VERIFIED".equalsIgnoreCase(user.getVerifyStatus()) || school.isEmpty()) {
            throw new RuntimeException("请先完成学号认证");
        }
        return school;
    }
}
