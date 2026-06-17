package com.campus.twohand.notice;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 公告列表分页查询
     */
    @GetMapping("/page")
    public ApiResp<?> page(@RequestParam int page, @RequestParam int size) {
        return noticeService.page(page, size);
    }

    /**
     * 公告详情查询
     */
    @GetMapping("/detail")
    public ApiResp<?> detail(@RequestParam Long id) {
        return noticeService.detail(id);
    }
}
