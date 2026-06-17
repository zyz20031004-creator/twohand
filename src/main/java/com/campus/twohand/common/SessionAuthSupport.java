package com.campus.twohand.common;

import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.SysUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionAuthSupport {

    private final SysUserRepository userRepo;

    public SessionAuthSupport(SysUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Long uid = parseLong(session.getAttribute("userId"));
        return uid != null && uid > 0 ? uid : null;
    }

    public Long requireUserId(HttpServletRequest request) {
        Long uid = currentUserId(request);
        if (uid == null) {
            throw new RuntimeException("请先登录");
        }
        return uid;
    }

    public SysUser requireAdmin(HttpServletRequest request) {
        Long uid = requireUserId(request);
        SysUser user = userRepo.findById(uid)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        if (!isAdmin(user)) {
            throw new RuntimeException("无权限操作");
        }
        return user;
    }

    public Long requireAdminId(HttpServletRequest request) {
        return requireAdmin(request).getId();
    }

    public SysUser requireSuperAdmin(HttpServletRequest request) {
        SysUser user = requireAdmin(request);
        if (!isSuperAdmin(user)) {
            throw new RuntimeException("仅超级管理员可操作");
        }
        return user;
    }

    public Long requireSuperAdminId(HttpServletRequest request) {
        return requireSuperAdmin(request).getId();
    }

    public boolean isAdmin(SysUser user) {
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return false;
        }
        return "ADMIN".equalsIgnoreCase(user.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(user.getRole());
    }

    public boolean isSuperAdmin(SysUser user) {
        if (user == null) {
            return false;
        }
        if ("admin".equalsIgnoreCase(trim(user.getUsername()))) {
            return true;
        }
        if (user.getId() != null && user.getId() == 13L) {
            return true;
        }
        return "SUPER_ADMIN".equalsIgnoreCase(user.getRole());
    }

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

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
