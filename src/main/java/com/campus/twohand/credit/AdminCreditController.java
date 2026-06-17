package com.campus.twohand.credit;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.credit.service.CreditService;
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
@RequestMapping("/api/admin/credit")
@RequiredArgsConstructor
public class AdminCreditController {

    private final CreditService creditService;

    /**
     * 用户信誉列表分页查询
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String verifyStatus,
                           @RequestParam(required = false) Integer minScore,
                           @RequestParam(required = false) Integer maxScore) {
        return creditService.adminPage(request, page, size, keyword, verifyStatus, minScore, maxScore);
    }

    /**
     * 用户信誉日志查询
     */
    @GetMapping("/logs")
    public ApiResp<?> logs(HttpServletRequest request,
                           @RequestParam Long userId,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int size) {
        return creditService.adminLogs(request, userId, page, size);
    }

    /**
     * 调整用户信誉分
     */
    @PostMapping("/adjust")
    public ApiResp<?> adjust(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return creditService.adjust(request, body);
    }
}
