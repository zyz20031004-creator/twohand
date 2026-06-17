package com.campus.twohand.category;

import com.campus.twohand.category.service.CategoryService;
import com.campus.twohand.common.ApiResp;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取商品分类列表
     */
    @GetMapping("/list")
    public ApiResp<?> list() {
        return categoryService.list();
    }
}
