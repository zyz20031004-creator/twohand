package com.campus.twohand.category.service;

import com.campus.twohand.category.AdminCategoryController;
import com.campus.twohand.category.entity.Category;
import com.campus.twohand.category.repo.CategoryRepository;
import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminCategoryService {

    private final CategoryRepository repo;
    private final SessionAuthSupport sessionAuthSupport;

    public AdminCategoryService(CategoryRepository repo, SessionAuthSupport sessionAuthSupport) {
        this.repo = repo;
        this.sessionAuthSupport = sessionAuthSupport;
    }

    public ApiResp<?> page(HttpServletRequest request,
                           int page,
                           int size,
                           String keyword,
                           Integer status) {
        sessionAuthSupport.requireAdminId(request);

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "id"));

        Specification<Category> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + keyword.trim() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Category> paged = repo.findAll(spec, pageable);
        return ApiResp.ok(new PageData<>(paged.getContent(), paged.getTotalElements()));
    }

    @Transactional
    public ApiResp<?> create(HttpServletRequest request, AdminCategoryController.SaveReq req) {
        sessionAuthSupport.requireAdminId(request);

        String name = req.getName() == null ? "" : req.getName().trim();
        validateName(name);
        if (repo.existsByName(name)) {
            throw new RuntimeException("分类名称已存在");
        }

        Category category = new Category();
        category.setName(name);
        category.setSort(0);
        category.setStatus(1);
        return ApiResp.ok(repo.save(category));
    }

    @Transactional
    public ApiResp<?> update(HttpServletRequest request, Long id, AdminCategoryController.SaveReq req) {
        sessionAuthSupport.requireAdminId(request);

        Category category = repo.findById(id).orElseThrow(() -> new RuntimeException("分类不存在"));
        String name = req.getName() == null ? "" : req.getName().trim();
        validateName(name);
        if (repo.existsByNameAndIdNot(name, id)) {
            throw new RuntimeException("分类名称已存在");
        }

        category.setName(name);
        return ApiResp.ok(repo.save(category));
    }

    @Transactional
    public ApiResp<?> delete(HttpServletRequest request, Long id) {
        sessionAuthSupport.requireAdminId(request);
        repo.deleteById(id);
        return ApiResp.ok(null);
    }

    @Transactional
    public ApiResp<?> batchDelete(HttpServletRequest request, AdminCategoryController.BatchDeleteReq req) {
        sessionAuthSupport.requireAdminId(request);
        if (req.getIds() == null || req.getIds().isEmpty()) {
            return ApiResp.ok(null);
        }
        repo.deleteAllById(req.getIds());
        return ApiResp.ok(null);
    }

    private void validateName(String name) {
        if (name.length() < 2 || name.length() > 30) {
            throw new RuntimeException("分类名称长度需在 2-30 个字符之间");
        }
    }

    @Getter
    public static class PageData<T> {
        private final List<T> list;
        private final long total;

        public PageData(List<T> list, long total) {
            this.list = list;
            this.total = total;
        }
    }
}
