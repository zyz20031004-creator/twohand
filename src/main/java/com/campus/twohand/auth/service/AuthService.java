package com.campus.twohand.auth.service;

import com.campus.twohand.auth.dto.LoginReq;
import com.campus.twohand.auth.dto.RegisterReq;
import com.campus.twohand.auth.dto.UserResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.SysUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_]{3,30}$";

    private final SysUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final SessionAuthSupport sessionAuthSupport;

    public AuthService(SysUserRepository userRepo,
                       PasswordEncoder passwordEncoder,
                       SessionAuthSupport sessionAuthSupport) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.sessionAuthSupport = sessionAuthSupport;
    }

    public UserResp register(RegisterReq req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String password = req.getPassword();

        if (username.isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (!username.matches(USERNAME_PATTERN)) {
            throw new RuntimeException("用户名长度需为 3 到 30 位，且只能包含英文、数字、下划线");
        }
        if (password == null || password.isBlank()) {
            throw new RuntimeException("密码不能为空");
        }
        if (password.length() < 6 || password.length() > 16) {
            throw new RuntimeException("密码长度需在 6 到 16 位之间");
        }
        if (userRepo.findByUsername(username).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setStatus(1);

        SysUser saved = userRepo.save(user);
        return new UserResp(saved.getId(), saved.getUsername(), saved.getRole());
    }

    public UserResp login(LoginReq req, HttpServletRequest request) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String password = req.getPassword();

        if (username.isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        SysUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("账号已禁用");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }

        if (req.getRole() != null && !req.getRole().trim().isEmpty()
                && !req.getRole().equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("角色不匹配");
        }

        request.getSession().setAttribute("userId", user.getId());
        request.getSession().setAttribute("role", user.getRole());

        return new UserResp(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getName(),
                user.getAvatar(),
                sessionAuthSupport.isSuperAdmin(user)
        );
    }
}
