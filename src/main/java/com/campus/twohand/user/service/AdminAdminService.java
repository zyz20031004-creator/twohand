package com.campus.twohand.user.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.SysUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 超级管理员后台 - 管理员管理服务
 * 提供针对管理员账号的增删改查等管理功能。
 * 所有操作都需要超级管理员权限。
 */
@Service
public class AdminAdminService {

    private final SysUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final SessionAuthSupport sessionAuthSupport;

    public AdminAdminService(SysUserRepository userRepo,
                             PasswordEncoder passwordEncoder,
                             SessionAuthSupport sessionAuthSupport) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.sessionAuthSupport = sessionAuthSupport;
    }

    /**
     * 分页查询管理员列表
     */
    public ApiResp<?> page(HttpServletRequest request, int page, int size, String keyword) {
        sessionAuthSupport.requireSuperAdmin(request);

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;

        String safeKeyword = nullableTrim(keyword);
        long total = userRepo.adminCount(safeKeyword);
        List<Map<String, Object>> rawRecords = userRepo.adminPage(safeKeyword, safeSize, offset);
        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> raw : rawRecords) {
            Map<String, Object> record = new LinkedHashMap<>(raw);
            SysUser user = userRepo.findById(parseLong(record.get("id"))).orElse(null);
            boolean superAdmin = sessionAuthSupport.isSuperAdmin(user);
            record.put("role", user == null ? "ADMIN" : user.getRole());
            record.put("isSuperAdmin", superAdmin);
            record.put("adminTypeText", superAdmin ? "超级管理员" : "普通管理员");
            records.add(record);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("records", records);
        return ApiResp.ok(data);
    }

    /**
     * 创建新的管理员账号
     */
    @Transactional
    public ApiResp<?> create(HttpServletRequest request, Map<String, Object> body) {
        sessionAuthSupport.requireSuperAdmin(request);

        String username = trim(value(body, "username"));
        String password = trim(value(body, "password"));

        if (username.isEmpty()) {
            return ApiResp.fail("username不能为空");
        }
        if ("admin".equalsIgnoreCase(username)) {
            return ApiResp.fail("超级管理员账号已保留");
        }
        if (password.isEmpty()) {
            return ApiResp.fail("password不能为空");
        }
        if (password.length() < 6 || password.length() > 20) {
            return ApiResp.fail("密码长度需在 6 到 20 位之间");
        }

        SysUser exists = userRepo.findByUsername(username).orElse(null);
        if (exists != null) {
            return ApiResp.fail("账号已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("ADMIN");
        user.setName(nullableTrim(value(body, "name")));
        user.setPhone(nullableTrim(value(body, "phone")));
        user.setEmail(nullableTrim(value(body, "email")));
        user.setAvatar(nullableTrim(value(body, "avatar")));
        Integer status = hasKey(body, "status") ? parseStatus(value(body, "status")) : 1;
        if (status == null) {
            return ApiResp.fail("状态值不合法");
        }
        user.setStatus(status);

        userRepo.save(user);
        return ApiResp.ok("ok");
    }

    /**
     * 更新管理员信息
     */
    @Transactional
    public ApiResp<?> update(HttpServletRequest request, Long id, Map<String, Object> body) {
        SysUser operator = sessionAuthSupport.requireSuperAdmin(request);

        SysUser user = userRepo.findById(id).orElse(null);
        if (user == null) {
            return ApiResp.fail("管理员不存在");
        }
        if (!sessionAuthSupport.isAdmin(user)) {
            return ApiResp.fail("非管理员不可编辑");
        }

        // 安全校验：针对超级管理员的特殊保护
        boolean targetSuperAdmin = sessionAuthSupport.isSuperAdmin(user);
        String password = trim(value(body, "password"));
        if (targetSuperAdmin) {
            if (hasKey(body, "status") && parseStatus(value(body, "status")) != null && parseStatus(value(body, "status")) != 1) {
                return ApiResp.fail("不能禁用超级管理员");
            }
            if (hasKey(body, "role") && !"ADMIN".equalsIgnoreCase(trim(value(body, "role")))) {
                return ApiResp.fail("不能修改超级管理员角色");
            }
            if (Objects.equals(operator.getId(), user.getId())) {
                return ApiResp.fail("不能修改当前超级管理员账号");
            }
        }

        if (targetSuperAdmin && !password.isEmpty()) {
            return ApiResp.fail("不能重置超级管理员密码");
        }

        // 更新字段
        if (hasKey(body, "name")) {
            user.setName(nullableTrim(value(body, "name")));
        }
        if (hasKey(body, "phone")) {
            user.setPhone(nullableTrim(value(body, "phone")));
        }
        if (hasKey(body, "email")) {
            user.setEmail(nullableTrim(value(body, "email")));
        }
        if (hasKey(body, "avatar")) {
            user.setAvatar(nullableTrim(value(body, "avatar")));
        }
        if (!targetSuperAdmin && hasKey(body, "status") && value(body, "status") != null) {
            Integer status = parseStatus(value(body, "status"));
            if (status == null) {
                return ApiResp.fail("状态值不合法");
            }
            user.setStatus(status);
        }
        if (!password.isEmpty()) {
            if (password.length() < 6 || password.length() > 20) {
                return ApiResp.fail("密码长度需在 6 到 20 位之间");
            }
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        userRepo.save(user);
        return ApiResp.ok("ok");
    }

    /**
     * 删除单个管理员
     */
    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        SysUser operator = sessionAuthSupport.requireSuperAdmin(request);

        SysUser user = userRepo.findById(id).orElse(null);
        if (user == null) {
            return ApiResp.ok("ok");
        }
        if (!sessionAuthSupport.isAdmin(user)) {
            return ApiResp.fail("非管理员不可删除");
        }
        if (sessionAuthSupport.isSuperAdmin(user) || Objects.equals(operator.getId(), user.getId())) {
            return ApiResp.fail("不能删除超级管理员或当前登录账号");
        }

        userRepo.deleteById(id);
        return ApiResp.ok("ok");
    }

    /**
     * 批量删除管理员
     */
    @Transactional
    public ApiResp<?> batchDelete(HttpServletRequest request, Map<String, Object> body) {
        SysUser operator = sessionAuthSupport.requireSuperAdmin(request);

        Object idsObj = value(body, "ids");
        if (!(idsObj instanceof List<?> list) || list.isEmpty()) {
            return ApiResp.ok("ok");
        }

        List<Long> ids = list.stream().map(this::parseLong).filter(Objects::nonNull).distinct().toList();
        List<SysUser> users = userRepo.findAllById(ids);
        for (SysUser user : users) {
            if (!sessionAuthSupport.isAdmin(user)) {
                return ApiResp.fail("非管理员不可删除");
            }
            if (sessionAuthSupport.isSuperAdmin(user) || Objects.equals(operator.getId(), user.getId())) {
                return ApiResp.fail("不能删除超级管理员或当前登录账号");
            }
        }

        userRepo.deleteAll(users);
        return ApiResp.ok("ok");
    }

    // ===== private helper methods =====

    /**
     * 从Map中安全地获取值
     */
    private Object value(Map<String, Object> body, String key) {
        return body == null ? null : body.get(key);
    }

    /**
     * 检查Map中是否存在某个key
     */
    private boolean hasKey(Map<String, Object> body, String key) {
        return body != null && body.containsKey(key);
    }

    /**
     * 将对象转为String并trim，null则返回空字符串
     */
    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 将对象转为String并trim，如果为空字符串则返回null
     */
    private String nullableTrim(Object value) {
        String text = trim(value);
        return text.isEmpty() ? null : text;
    }

    /**
     * 安全地将对象解析为Long
     */
    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 安全地将对象解析为状态值 (0或1)
     */
    private Integer parseStatus(Object value) {
        if (value == null) {
            return null;
        }
        try {
            int status = Integer.parseInt(String.valueOf(value).trim());
            return (status == 0 || status == 1) ? status : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
