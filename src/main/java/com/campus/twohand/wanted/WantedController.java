package com.campus.twohand.wanted;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.wanted.service.WantedService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wanted")
@RequiredArgsConstructor
public class WantedController {

    private final WantedService wantedService;

    /**
     * 求购列表分页查询
     */
    @GetMapping("/page")
    public ApiResp<?> page(HttpServletRequest request,
                           @RequestParam int page,
                           @RequestParam int size,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String keyword) {
        return wantedService.page(request, page, size, status, keyword);
    }

    /**
     * 我的求购列表
     */
    @GetMapping("/my/page")
    public ApiResp<?> myPage(HttpServletRequest request,
                             @RequestParam int page,
                             @RequestParam int size,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String keyword) {
        return wantedService.myPage(request, page, size, status, keyword);
    }

    /**
     * 创建求购信息
     */
    @PostMapping("/create")
    public ApiResp<?> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return wantedService.create(request, body);
    }

    /**
     * 更新求购信息
     */
    @PutMapping("/update/{id}")
    public ApiResp<?> update(HttpServletRequest request,
                             @PathVariable Long id,
                             @RequestBody Map<String, Object> body) {
        return wantedService.update(request, id, body);
    }

    /**
     * 删除求购信息
     */
    @DeleteMapping("/delete/{id}")
    public ApiResp<?> delete(HttpServletRequest request, @PathVariable Long id) {
        return wantedService.delete(request, id);
    }

    /**
     * 标记求购已解决
     */
    @PutMapping("/solve/{id}")
    public ApiResp<?> solve(HttpServletRequest request, @PathVariable Long id) {
        return wantedService.solve(request, id);
    }

    /**
     * 增加浏览量
     */
    @PostMapping("/{id}/view")
    public ApiResp<?> increaseViewCount(@PathVariable Long id) {
        return wantedService.increaseViewCount(id);
    }
}
