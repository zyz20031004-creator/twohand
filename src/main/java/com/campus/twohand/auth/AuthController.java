package com.campus.twohand.auth;

import com.campus.twohand.auth.dto.LoginReq;
import com.campus.twohand.auth.dto.RegisterReq;
import com.campus.twohand.auth.dto.UserResp;
import com.campus.twohand.auth.service.AuthService;
import com.campus.twohand.common.ApiResp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册接口
     */
    @PostMapping("/register")
    public ApiResp<UserResp> register(@RequestBody RegisterReq req) {
        return ApiResp.ok(authService.register(req));
    }

    /**
     * 用户登录接口
     */
    @PostMapping("/login")
    public ApiResp<UserResp> login(@RequestBody LoginReq req, HttpServletRequest request) {
        return ApiResp.ok(authService.login(req, request));
    }
}
