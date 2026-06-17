package com.campus.twohand.product.repo;

import com.campus.twohand.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 鍟嗗搧 Repository
 */
public interface ProductRepository
        extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    /**
     * 鍟嗗搧璇︽儏鑱旇〃鏌ヨ
     * 鑱?product + sys_user
     */
    @Query("""
        select
            p.id as id,
            p.title as title,
            p.price as price,
            p.addressText as addressText,
            p.schoolName as schoolName,
            u.name as sellerName,
            u.avatar as sellerAvatar,
            u.phone as sellerPhone,
            p.description as description,
            p.viewCount as viewCount,
            p.likeCount as likeCount,
            p.createdAt as createdAt
        from Product p
        left join SysUser u on p.sellerId = u.id
        where p.id = :id
    """)
    ProductDetailView findDetailById(@Param("id") Long id);


//   admin
    @Query(value = """
        SELECT
          p.id                           AS id,
          p.title                        AS name,
          p.price                        AS price,
          p.description                  AS description,
          p.address_text                 AS shipAddress,
          p.school_name                  AS schoolName,
          DATE_FORMAT(p.created_at, '%Y-%m-%d') AS createdAt,
          p.audit_status                 AS auditStatus,
          p.audit_reason                 AS auditReason,
          p.status                       AS saleStatus,
          COALESCE(p.sold_flag, 0)       AS soldFlag,
          u.nick_name                    AS sellerNickName,
          u.username                     AS sellerUsername,
          COALESCE(u.nick_name, u.username)   AS username,
          c.name                         AS categoryName
        FROM product p
        LEFT JOIN sys_user u ON u.id = p.seller_id
        LEFT JOIN category c ON c.id = p.category_id
        WHERE 1=1
          AND (:kw IS NULL OR :kw = '' OR
               p.title LIKE CONCAT('%', :kw, '%')
               OR COALESCE(u.nick_name, u.username) LIKE CONCAT('%', :kw, '%')
               OR p.school_name LIKE CONCAT('%', :kw, '%')
               OR c.name LIKE CONCAT('%', :kw, '%')
          )
          AND (:school IS NULL OR :school = '' OR p.school_name LIKE CONCAT('%', :school, '%'))
          AND (:auditStatus IS NULL OR :auditStatus = '' OR p.audit_status = :auditStatus)
          AND (:saleStatus IS NULL OR :saleStatus = '' OR p.status = :saleStatus)
        ORDER BY p.id DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Map<String, Object>> adminPage(
            @Param("kw") String kw,
            @Param("school") String school,
            @Param("auditStatus") String auditStatus,
            @Param("saleStatus") String saleStatus,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(1)
        FROM product p
        LEFT JOIN sys_user u ON u.id = p.seller_id
        LEFT JOIN category c ON c.id = p.category_id
        WHERE 1=1
          AND (:kw IS NULL OR :kw = '' OR
          p.title LIKE CONCAT('%', :kw, '%')
          OR COALESCE(u.nick_name, u.username) LIKE CONCAT('%', :kw, '%')
          OR p.school_name LIKE CONCAT('%', :kw, '%')
          OR c.name LIKE CONCAT('%', :kw, '%')
          )
          AND (:school IS NULL OR :school = '' OR p.school_name LIKE CONCAT('%', :school, '%'))
          AND (:auditStatus IS NULL OR :auditStatus = '' OR p.audit_status = :auditStatus)
          AND (:saleStatus IS NULL OR :saleStatus = '' OR p.status = :saleStatus)
        """, nativeQuery = true)
    long adminCount(@Param("kw") String kw,
                    @Param("school") String school,
                    @Param("auditStatus") String auditStatus,
                    @Param("saleStatus") String saleStatus);

    @Query(value = """
        SELECT COUNT(1)
        FROM product p
        WHERE p.status = 'ON'
          AND p.audit_status = 'APPROVED'
          AND COALESCE(p.sold_flag, 0) = 0
        """, nativeQuery = true)
    long countOnSaleForDashboard();

    long countByAuditStatus(String auditStatus);

    @Query(value = """
        SELECT
          DATE_FORMAT(days.day_date, '%m-%d') AS label,
          COUNT(p.id) AS value
        FROM (
          SELECT CURDATE() - INTERVAL 6 DAY AS day_date
          UNION ALL SELECT CURDATE() - INTERVAL 5 DAY
          UNION ALL SELECT CURDATE() - INTERVAL 4 DAY
          UNION ALL SELECT CURDATE() - INTERVAL 3 DAY
          UNION ALL SELECT CURDATE() - INTERVAL 2 DAY
          UNION ALL SELECT CURDATE() - INTERVAL 1 DAY
          UNION ALL SELECT CURDATE()
        ) days
        LEFT JOIN product p
          ON DATE(p.created_at) = days.day_date
        GROUP BY days.day_date
        ORDER BY days.day_date
        """, nativeQuery = true)
    List<Map<String, Object>> dashboardPublishTrend();

    @Query(value = """
            SELECT
              p.id                           AS id,
              p.title                        AS name,
              p.price                        AS price,
              p.description                  AS description,
              p.address_text                 AS shipAddress,
              p.school_name                  AS schoolName,
              DATE_FORMAT(p.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt,
              p.audit_status                 AS auditStatus,
              p.audit_reason                 AS auditReason,
              p.status                       AS saleStatus,
              COALESCE(p.sold_flag, 0)       AS soldFlag,
              p.seller_id                    AS userId,
              u.nick_name                    AS sellerNickName,
              u.username                     AS sellerUsername,
              COALESCE(u.nick_name, u.username)   AS username,
              c.name                         AS categoryName
            FROM product p
            LEFT JOIN sys_user u ON u.id = p.seller_id
            LEFT JOIN category c ON c.id = p.category_id
            WHERE p.id = :id
            LIMIT 1
            """, nativeQuery = true)
    Map<String, Object> adminDetail(@Param("id") Long id);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    List<Product> findBySellerIdOrderByIdAsc(Long sellerId);

    Optional<Product> findFirstByCategoryIdAndTitleOrderByIdAsc(Long categoryId, String title);

    @Query(value = "SELECT * FROM product WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query(value = """
            UPDATE product
            SET school_name = :schoolName
            WHERE seller_id = :sellerId
              AND COALESCE(sold_flag, 0) = 0
            """, nativeQuery = true)
    int updateUnsoldSchoolNameBySellerId(@Param("sellerId") Long sellerId,
                                         @Param("schoolName") String schoolName);

    /**
     * 鍟嗗搧璇︽儏鎶曞奖鎺ュ彛
     * 鍙繑鍥炴垜浠渶瑕佺殑瀛楁
     */
    interface ProductDetailView {

        Long getId();

        String getTitle();

        java.math.BigDecimal getPrice();

        String getAddressText();

        String getSchoolName();

        String getSellerName();

        String getSellerAvatar();

        String getSellerPhone();

        String getDescription();

        Integer getViewCount();

        Integer getLikeCount();

        java.time.LocalDateTime getCreatedAt();
    }
}
