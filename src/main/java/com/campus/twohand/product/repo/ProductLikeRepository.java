package com.campus.twohand.product.repo;

import com.campus.twohand.product.entity.ProductLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {

    Optional<ProductLike> findByUserIdAndProductId(Long userId, Long productId);

    long countByProductIdAndStatus(Long productId, Integer status);

    public interface ProductLikeCountView {
        Long getProductId();
        Long getCnt();
    }

    @Query("""
       select pl.productId as productId, count(pl.id) as cnt
       from ProductLike pl
       where pl.status = 1 and pl.productId in :ids
       group by pl.productId
       """)
    List<ProductLikeCountView> countLikesByProductIds(@Param("ids") List<Long> ids);


}

