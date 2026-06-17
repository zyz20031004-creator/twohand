package com.campus.twohand.user;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.user.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * 获取当前登录用户的个人信息
     */
    @GetMapping("/me")
    public ApiResp<?> me(HttpServletRequest request) {
        return userProfileService.me(request);
    }

    /**
     * 修改当前登录用户的个人信息
     */
    @PutMapping("/me")
    public ApiResp<?> updateMe(HttpServletRequest request, @RequestBody UpdateMeReq req) {
        return userProfileService.updateMe(request, req.name, req.phone, req.email, req.avatar);
    }

    /**
     * 修改当前登录用户的密码
     */
    @PostMapping("/changePassword")
    public ApiResp<?> changePassword(HttpServletRequest request, @RequestBody ChangePwdReq req) {
        return userProfileService.changePassword(request, req.oldPwd, req.newPwd, req.confirmPwd);
    }

    /**
     * 获取指定用户的公开资料（主页）
     */
    @GetMapping("/profile/{id}")
    public ApiResp<?> publicProfile(@PathVariable Long id) {
        return userProfileService.publicProfile(id);
    }

    /**
     * 获取指定用户发布的商品列表
     */
    @GetMapping("/profile/{id}/products")
    public ApiResp<?> publicProducts(@PathVariable Long id,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "8") int size,
                                     @RequestParam(required = false) String status) {
        return userProfileService.publicProducts(id, page, size, status);
    }

    /**
     * 获取指定用户的信誉评价记录
     */
    @GetMapping("/profile/{id}/credit")
    public ApiResp<?> publicCredit(@PathVariable Long id,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String filter) {
        return userProfileService.publicCredit(id, page, size, filter);
    }

    public static class UpdateMeReq {
        public String name;
        public String phone;
        public String email;
        public String avatar;
    }

    public static class ChangePwdReq {
        public String oldPwd;
        public String newPwd;
        public String confirmPwd;
    }
}
