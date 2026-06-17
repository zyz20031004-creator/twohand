package com.campus.twohand.verify.service;

import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VerifyGuard {

    private final SysUserRepository sysUserRepository;

    public void requireVerified(Long uid) {
        SysUser user = sysUserRepository.findById(uid).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!"VERIFIED".equalsIgnoreCase(user.getVerifyStatus())) {
            throw new RuntimeException("4001:请先完成学号认证");
        }
    }
}
