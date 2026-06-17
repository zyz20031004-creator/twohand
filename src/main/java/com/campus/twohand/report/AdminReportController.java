package com.campus.twohand.report;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.report.service.ReportService;
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
@RequestMapping("/api/admin/report")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    /**
     * 举报管理列表分页查询
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        return reportService.adminPage(request, page, size, keyword, status);
    }

    /**
     * 处理举报
     */
    @PostMapping("/handle")
    public ApiResp<?> handle(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return reportService.handle(request, body);
    }
}
