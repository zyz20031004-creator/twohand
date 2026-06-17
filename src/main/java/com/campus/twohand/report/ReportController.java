package com.campus.twohand.report;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.report.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 我的举报列表
     */
    @GetMapping("/my/page")
    public ApiResp<?> myPage(HttpServletRequest request,
                             @RequestHeader(value = "X-User-Id", required = false) Long uid,
                             @RequestParam int page,
                             @RequestParam int size,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status) {
        return reportService.myPage(request, uid, page, size, keyword, status);
    }

    /**
     * 提交举报
     */
    @PostMapping("/submit")
    public ApiResp<?> submit(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return reportService.submit(request, body);
    }
}
