package com.campus.twohand.feedback;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.feedback.service.AdminFeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/feedback")
public class AdminFeedbackController {

    private final AdminFeedbackService adminFeedbackService;

    public AdminFeedbackController(AdminFeedbackService adminFeedbackService) {
        this.adminFeedbackService = adminFeedbackService;
    }

    /**
     * 反馈管理列表分页查询
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String keyword) {
        return adminFeedbackService.page(request, page, size, keyword);
    }

    /**
     * 回复反馈
     */
    @PutMapping("/{id}/reply")
    public ApiResp<?> reply(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return adminFeedbackService.reply(request, id, body);
    }

    /**
     * 删除反馈
     */
    @DeleteMapping("/{id}")
    public ApiResp<?> delete(HttpServletRequest request, @PathVariable Long id) {
        return adminFeedbackService.delete(request, id);
    }

    /**
     * 批量删除反馈
     */
    @DeleteMapping("/batch")
    public ApiResp<?> batchDelete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return adminFeedbackService.batchDelete(request, body);
    }
}
