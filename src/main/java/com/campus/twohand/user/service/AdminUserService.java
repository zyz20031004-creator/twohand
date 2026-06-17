package com.campus.twohand.user.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.user.entity.AdminAuditLog;
import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.AdminAuditLogRepository;
import com.campus.twohand.user.repo.SysUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员后台 - 用户管理服务
 */
@Service
public class AdminUserService {

    private final SysUserRepository userRepo;
    private final AdminAuditLogRepository auditLogRepo;
    private final SessionAuthSupport sessionAuthSupport;

    public AdminUserService(SysUserRepository userRepo,
                            AdminAuditLogRepository auditLogRepo,
                            SessionAuthSupport sessionAuthSupport) {
        this.userRepo = userRepo;
        this.auditLogRepo = auditLogRepo;
        this.sessionAuthSupport = sessionAuthSupport;
    }

    /**
     * 分页查询普通用户列表
     */
    public ApiResp<?> page(HttpServletRequest request, int page, int size, String keyword) {
        requireAdmin(request);

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;

        long total = userRepo.userCount(keyword);
        List<Map<String, Object>> records = userRepo.userPage(keyword, safeSize, offset);

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("records", records);
        return ApiResp.ok(data);
    }

    /**
     * 创建用户（已禁用）
     */
    @Transactional
    public ApiResp<?> create(HttpServletRequest request, Map<String, Object> body) {
        requireAdmin(request);
        return ApiResp.fail("后台不允许新增普通用户，请用户自行注册");
    }

    /**
     * 更新用户信息（仅限状态）
     */
    @Transactional
    public ApiResp<?> update(HttpServletRequest request, Long id, Map<String, Object> body) {
        SysUser operator = requireAdmin(request);
        SysUser target = userRepo.findById(id).orElse(null);
        if (target == null) {
            return ApiResp.fail("用户不存在");
        }

        ApiResp<?> guardFail = ensureUserTarget(target);
        if (guardFail != null) {
            return guardFail;
        }

        ApiResp<?> fieldFail = ensureOnlyStatusField(body);
        if (fieldFail != null) {
            return fieldFail;
        }

        Integer status = parseStatus(value(body, "status"));
        if (status == null) {
            return ApiResp.fail("状态值不合法");
        }

        String before = toJson(snapshot(target));
        target.setStatus(status);
        userRepo.save(target);
        String action = status == 1 ? "ENABLE" : "DISABLE";
        writeAudit(operator, target.getId(), action, before, toJson(snapshot(target)), request);
        return ApiResp.ok("ok");
    }

    /**
     * 更改用户状态（启用/禁用）
     */
    @Transactional
    public ApiResp<?> changeStatus(HttpServletRequest request, Long id, Map<String, Object> body) {
        SysUser operator = requireAdmin(request);
        SysUser target = userRepo.findById(id).orElse(null);
        if (target == null) {
            return ApiResp.fail("用户不存在");
        }

        ApiResp<?> guardFail = ensureUserTarget(target);
        if (guardFail != null) {
            return guardFail;
        }

        Integer status = parseStatus(value(body, "status"));
        if (status == null) {
            return ApiResp.fail("状态值不合法");
        }

        String before = toJson(snapshot(target));
        target.setStatus(status);
        userRepo.save(target);

        String action = status == 1 ? "ENABLE" : "DISABLE";
        writeAudit(operator, target.getId(), action, before, toJson(snapshot(target)), request);
        return ApiResp.ok("ok");
    }

    /**
     * 批量更改用户状态
     */
    @Transactional
    public ApiResp<?> batchChangeStatus(HttpServletRequest request, Map<String, Object> body) {
        SysUser operator = requireAdmin(request);

        Object idsObj = value(body, "ids");
        if (!(idsObj instanceof List<?> rawIds) || rawIds.isEmpty()) {
            return ApiResp.fail("请选择要操作的用户");
        }

        Integer status = parseStatus(value(body, "status"));
        if (status == null) {
            return ApiResp.fail("状态值不合法");
        }

        List<Long> ids = rawIds.stream().map(item -> Long.valueOf(String.valueOf(item))).distinct().toList();
        List<SysUser> targets = userRepo.findAllById(ids);
        if (targets.size() != ids.size()) {
            return ApiResp.fail("存在无效用户");
        }

        for (SysUser target : targets) {
            ApiResp<?> guardFail = ensureUserTarget(target);
            if (guardFail != null) {
                return guardFail;
            }
        }

        String action = status == 1 ? "ENABLE" : "DISABLE";
        for (SysUser target : targets) {
            String before = toJson(snapshot(target));
            target.setStatus(status);
            userRepo.save(target);
            writeAudit(operator, target.getId(), action, before, toJson(snapshot(target)), request);
        }

        return ApiResp.ok("ok");
    }

    /**
     * 删除用户（已禁用）
     */
    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        requireAdmin(request);
        return ApiResp.fail("已禁止删除，请改用禁用/启用");
    }

    // ===== private helper methods =====

    /**
     * 验证当前会话是否为管理员，并返回操作员实体
     */
    private SysUser requireAdmin(HttpServletRequest request) {
        Long adminId = sessionAuthSupport.requireAdminId(request);
        SysUser operator = userRepo.findById(adminId).orElse(null);
        if (operator == null) {
            throw new RuntimeException("管理员不存在");
        }
        return operator;
    }

    /**
     * 确保操作目标是普通用户（非管理员）
     */
    private ApiResp<?> ensureUserTarget(SysUser target) {
        if (target == null) {
            return ApiResp.fail("用户不存在");
        }
        if (!"USER".equalsIgnoreCase(trim(target.getRole()))) {
            return ApiResp.fail("管理员账号请在管理员信息管理中处理");
        }
        return null;
    }

    /**
     * 安全地将对象解析为状态值 (0或1)
     */
    private Integer parseStatus(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean boolValue) {
            return boolValue ? 1 : 0;
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }

        String normalized = text.toUpperCase();
        switch (normalized) {
            case "1":
            case "TRUE":
            case "NORMAL":
            case "ENABLED":
                return 1;
            case "0":
            case "FALSE":
            case "DISABLED":
                return 0;
            default:
                return null;
        }
    }

    /**
     * 将对象转为String并trim，null则返回空字符串
     */
    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

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
     * 将对象转为String并trim，如果为空字符串则返回null
     */
    private String nullableTrim(Object value) {
        String text = trim(value);
        return text.isEmpty() ? null : text;
    }

    /**
     * 确保请求体中只包含 "status" 字段
     */
    private ApiResp<?> ensureOnlyStatusField(Map<String, Object> body) {
        if (body == null) {
            return ApiResp.fail("状态值不合法");
        }
        if (!body.containsKey("status")) {
            return ApiResp.fail("状态值不合法");
        }
        for (String field : body.keySet()) {
            if (!"status".equals(field)) {
                return ApiResp.fail("后台不允许修改用户个人资料");
            }
        }
        return null;
    }

    /**
     * 创建用户数据的快照，用于审计日志
     */
    private Map<String, Object> snapshot(SysUser user) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("name", user.getName());
        data.put("phone", user.getPhone());
        data.put("email", user.getEmail());
        data.put("avatar", user.getAvatar());
        data.put("status", user.getStatus());
        return data;
    }

    /**
     * 将对象手动序列化为JSON字符串，用于审计日志
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append('"').append(escapeJson(String.valueOf(entry.getKey()))).append('"').append(':');
                Object item = entry.getValue();
                if (item == null) {
                    builder.append("null");
                } else if (item instanceof Number || item instanceof Boolean) {
                    builder.append(item);
                } else {
                    builder.append('"').append(escapeJson(String.valueOf(item))).append('"');
                }
            }
            builder.append('}');
            return builder.toString();
        }
        return '"' + escapeJson(String.valueOf(value)) + '"';
    }

    /**
     * 对JSON字符串中的特殊字符进行转义
     */
    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    /**
     * 记录管理员操作审计日志
     */
    private void writeAudit(SysUser operator,
                            Long targetUserId,
                            String action,
                            String beforeData,
                            String afterData,
                            HttpServletRequest request) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminId(operator.getId());
        log.setTargetUserId(targetUserId);
        log.setAction(action);
        log.setBeforeData(beforeData);
        log.setAfterData(afterData);
        log.setIp(resolveIp(request));
        auditLogRepo.save(log);
    }

    /**
     * 从HTTP请求中解析客户端IP地址
     */
    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
