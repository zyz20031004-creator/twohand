package com.campus.twohand.system;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.system.service.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class AdminSystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 获取系统配置
     */
    @GetMapping("/config")
    public ApiResp<?> getConfig(HttpServletRequest request) {
        return systemConfigService.get(request);
    }

    /**
     * 保存系统配置
     */
    @PutMapping("/config")
    public ApiResp<?> saveConfig(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return systemConfigService.save(request, body);
    }
}
