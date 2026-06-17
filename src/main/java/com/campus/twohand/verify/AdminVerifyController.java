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

import java.util.Map;

@RestController
@RequestMapping("/api/admin/verify")
@RequiredArgsConstructor
public class AdminVerifyController {

    private final StudentVerifyService studentVerifyService;

    /**
     * 认证申请列表分页查询
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String keyword) {
        return studentVerifyService.adminPage(request, page, size, status, keyword);
    }

    /**
     * 审核通过认证申请
     */
    @PostMapping("/approve")
    public ApiResp<?> approve(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return studentVerifyService.adminApprove(request, body);
    }

    /**
     * 驳回认证申请
     */
    @PostMapping("/reject")
    public ApiResp<?> reject(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return studentVerifyService.adminReject(request, body);
    }
}
