package com.campus.twohand.product.repo;

import com.campus.twohand.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 商品图片 Repository
 */
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /**
     * 查询单个商品的图片
     */
    List<ProductImage> findByProductIdOrderBySortAscIdAsc(Long productId);

    /**
     * 批量查询多个商品的图片
     *
     * product_id IN (...)
     * ORDER BY product_id ASC, sort ASC
     *
     * 用于分页接口优化（避免N+1查询）
     */
    List<ProductImage> findByProductIdInOrderByProductIdAscSortAscIdAsc(List<Long> productIds);

    List<ProductImage> findByProductIdOrderBySortAsc(Long productId);

    void deleteByProductId(Long productId);
}
