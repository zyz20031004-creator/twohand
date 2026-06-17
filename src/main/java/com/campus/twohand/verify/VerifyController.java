package com.campus.twohand.verify;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.verify.service.StudentVerifyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
public class VerifyController {

    private final StudentVerifyService studentVerifyService;

    /**
     * 提交学号认证申请
     */
    @PostMapping("/apply")
    public ApiResp<?> apply(HttpServletRequest request, @RequestBody Map<String, Object> req) {
        return studentVerifyService.apply(request, req);
    }

    /**
     * 查询当前用户认证状态
     */
    @GetMapping("/my")
    public ApiResp<?> my(HttpServletRequest request) {
        return studentVerifyService.my(request);
    }

    /**
     * 上传认证材料
     */
    @PostMapping("/upload")
    public ApiResp<?> upload(HttpServletRequest request, @RequestParam("file") MultipartFile file) throws IOException {
        return studentVerifyService.uploadMaterial(request, file);
    }
}
