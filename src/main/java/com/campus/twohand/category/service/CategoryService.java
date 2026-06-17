package com.campus.twohand.category.service;

import com.campus.twohand.category.repo.CategoryRepository;
import com.campus.twohand.common.ApiResp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public ApiResp<?> list() {
        return ApiResp.ok(categoryRepository.findAllByOrderBySortAscIdAsc());
    }
}
