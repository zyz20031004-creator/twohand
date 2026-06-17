package com.campus.twohand.product.repo;

import com.campus.twohand.product.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // 查某用户是否收藏过某商品
    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);

    // 统计某商品有效收藏数（status=1）
    long countByProductIdAndStatus(Long productId, Integer status);

    // 批量统计多个商品的收藏数
    @Query(value = "SELECT product_id as productId, COUNT(*) as cnt FROM favorite WHERE product_id IN (:productIds) AND status = 1 GROUP BY product_id", nativeQuery = true)
    List<Map<String, Object>> countFavoritesByProductIds(@Param("productIds") List<Long> productIds);

    List<Favorite> findByUserIdAndStatusOrderByIdDesc(Long userId, Integer status);


    @Modifying
    @Query(value = "UPDATE favorite SET status = 0 WHERE user_id = :uid AND product_id IN (:pids)", nativeQuery = true)
    int batchCancel(@Param("uid") Long uid, @Param("pids") List<Long> pids);

//    我的收藏分页
    @Query(
            value = """
    SELECT
      f.id          AS id,
      f.product_id  AS productId,
      p.title       AS title,
      p.price       AS price,
      p.status      AS productStatus,
      p.audit_status AS auditStatus,
      p.sold_flag   AS soldFlag,
      f.created_at  AS createdAt,
      (
        SELECT pi.url
        FROM product_image pi
        WHERE pi.product_id = p.id
        ORDER BY pi.sort ASC, pi.id ASC
        LIMIT 1
      ) AS coverUrl
    FROM favorite f
    JOIN product p ON p.id = f.product_id
    WHERE f.user_id = :uid AND f.status = 1
      AND (:kw IS NULL OR :kw = '' OR COALESCE(p.title,'') LIKE CONCAT('%', :kw, '%'))
    ORDER BY f.created_at DESC
  """,
            countQuery = """
    SELECT COUNT(1)
    FROM favorite f
    JOIN product p ON p.id = f.product_id
    WHERE f.user_id = :uid AND f.status = 1
      AND (:kw IS NULL OR :kw = '' OR COALESCE(p.title,'') LIKE CONCAT('%', :kw, '%'))
  """,
            nativeQuery = true
    )
    Page<java.util.Map<String, Object>> userFavPage(
            @Param("uid") Long userId,
            @Param("kw") String keyword,
            Pageable pageable
    );
}
