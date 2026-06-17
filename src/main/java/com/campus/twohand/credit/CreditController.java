package com.campus.twohand.credit;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.credit.service.CreditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    /**
     * 获取当前用户信誉信息
     */
    @GetMapping("/my")
    public ApiResp<?> my(HttpServletRequest request,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int size) {
        return creditService.my(request, page, size);
    }

    /**
     * 获取当前用户评价记录
     */
    @GetMapping("/my/reviews")
    public ApiResp<?> myReviews(HttpServletRequest request,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size) {
        return creditService.myReviews(request, page, size);
    }
}
