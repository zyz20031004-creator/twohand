package com.campus.twohand.feedback;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.feedback.service.AdminFeedbackService;
import com.campus.twohand.feedback.service.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final AdminFeedbackService adminFeedbackService;

    /**
     * 提交反馈
     */
    @PostMapping("/submit")
    public ApiResp<?> submit(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return feedbackService.submit(request, body);
    }

    /**
     * 反馈列表分页查询（旧版）
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size) {
        return adminFeedbackService.pageLegacy(request, page, size);
    }

    /**
     * 回复反馈（旧版）
     */
    @PostMapping("/reply")
    public ApiResp<?> reply(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return adminFeedbackService.replyLegacy(request, body);
    }

    /**
     * 关闭反馈（旧版）
     */
    @PostMapping("/close")
    public ApiResp<?> close(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return adminFeedbackService.closeLegacy(request, body);
    }

    /**
     * 我的反馈列表
     */
    @GetMapping("/my/page")
    public ApiResp<?> myPage(HttpServletRequest request,
                             @RequestParam int page,
                             @RequestParam int size,
                             @RequestParam(required = false) Long userId,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String keyword) {
        return feedbackService.myPage(request, page, size, status, keyword);
    }

    /**
     * 删除反馈
     */
    @DeleteMapping("/delete/{id}")
    public ApiResp<?> delete(HttpServletRequest request,
                             @PathVariable Long id,
                             @RequestParam(required = false) Long userId) {
        return feedbackService.delete(request, id);
    }
}
