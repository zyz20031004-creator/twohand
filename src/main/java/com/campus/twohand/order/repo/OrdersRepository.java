package com.campus.twohand.order.repo;

import com.campus.twohand.order.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    long countByStatus(String status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByProductId(Long productId);

    boolean existsByProductIdAndStatus(Long productId, String status);

    boolean existsByProductIdAndStatusNot(Long productId, String status);

    List<Orders> findByStatusAndCreatedAtLessThanEqual(String status, LocalDateTime deadline);

    /**
     * 缁狅紕鎮婇崨妯款吂閸楁洖鍨庢い纰夌窗keyword 閺€顖涘瘮閸栧綊鍘?
     * 鐠併垹宕熼崣鏋偓浣告櫌閸濅焦鐖ｆ０妯糕偓浣锋嫳鐎?閸楁牕顔嶉悽銊﹀煕閸氬秵鍨ㄩ弰鐢敌為妴浣告勾閸р偓閺傚洦婀伴妴浣瑰閺堝搫褰?
     * 鏉╂柨娲?Map 閺傞€涚┒韫囶偊鈧喎顕幒銉ュ缁?
     */
    @Query(
            value = """
            SELECT
              o.id                 AS id,
              o.order_no           AS orderNo,
              o.product_id         AS productId,
              o.product_title      AS productTitle,
              o.amount             AS amount,
              o.status             AS status,
              o.buyer_id           AS buyerId,
              COALESCE(NULLIF(TRIM(bu.nick_name), ''), NULLIF(TRIM(bu.username), ''), '校园用户') AS buyerName,
              COALESCE(bu.avatar, '') AS buyerAvatar,
              o.seller_id          AS sellerId,
              COALESCE(NULLIF(TRIM(su.nick_name), ''), NULLIF(TRIM(su.username), ''), '校园用户') AS sellerName,
              COALESCE(su.avatar, '') AS sellerAvatar,
              o.address_id         AS addressId,
              COALESCE(NULLIF(TRIM(o.contact_name), ''), ua.contact_name, '-') AS receiver,
              COALESCE(NULLIF(TRIM(o.contact_phone), ''), ua.contact_phone, '-') AS phone,
              COALESCE(NULLIF(TRIM(o.address_text), ''), ua.address_text, '-') AS receiveAddress,
              COALESCE(NULLIF(TRIM(o.trade_location), ''), NULLIF(TRIM(p.address_text), ''), '-') AS tradeLocation,
              o.created_at         AS createdAt,
              CASE
                WHEN o.status = 'PAID' AND o.paid_at IS NULL THEN o.created_at
                ELSE o.paid_at
              END AS paidAt,
              o.pay_type           AS payType,
              o.cancelled_at       AS cancelledAt,
              o.finished_at        AS finishedAt,
              (
                SELECT pi.url
                FROM product_image pi
                WHERE pi.product_id = o.product_id
                ORDER BY pi.sort ASC, pi.id ASC
                LIMIT 1
              ) AS coverUrl
            FROM orders o
            LEFT JOIN product p ON p.id = o.product_id
            LEFT JOIN sys_user bu ON bu.id = o.buyer_id
            LEFT JOIN sys_user su ON su.id = o.seller_id
            LEFT JOIN user_address ua ON ua.id =
                COALESCE(
                    o.address_id,
                    (SELECT ua2.id
                     FROM user_address ua2
                     WHERE ua2.user_id = o.buyer_id AND ua2.status = 1
                     ORDER BY ua2.is_default DESC, ua2.id DESC
                     LIMIT 1)
                )
            WHERE
              (:kw IS NULL OR :kw = '' OR
                COALESCE(o.order_no,'')      LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.product_title,'') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(bu.username,'')     LIKE CONCAT('%', :kw, '%') OR
                COALESCE(bu.nick_name,'')         LIKE CONCAT('%', :kw, '%') OR
                COALESCE(su.username,'')     LIKE CONCAT('%', :kw, '%') OR
                COALESCE(su.nick_name,'')         LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.address_text, ua.address_text, '') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.trade_location,'') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(p.address_text,'') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.contact_phone, ua.contact_phone, '')LIKE CONCAT('%', :kw, '%')
              )
            ORDER BY o.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(1)
            FROM orders o
            LEFT JOIN product p ON p.id = o.product_id
            LEFT JOIN sys_user bu ON bu.id = o.buyer_id
            LEFT JOIN sys_user su ON su.id = o.seller_id
            LEFT JOIN user_address ua ON ua.id =
                COALESCE(
                    o.address_id,
                    (SELECT ua2.id
                     FROM user_address ua2
                     WHERE ua2.user_id = o.buyer_id AND ua2.status = 1
                     ORDER BY ua2.is_default DESC, ua2.id DESC
                     LIMIT 1)
                )
            WHERE
              (:kw IS NULL OR :kw = '' OR
                COALESCE(o.order_no,'')      LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.product_title,'') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(bu.username,'')     LIKE CONCAT('%', :kw, '%') OR
                COALESCE(bu.nick_name,'')         LIKE CONCAT('%', :kw, '%') OR
                COALESCE(su.username,'')     LIKE CONCAT('%', :kw, '%') OR
                COALESCE(su.nick_name,'')         LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.address_text, ua.address_text, '') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.trade_location,'') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(p.address_text,'') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.contact_phone, ua.contact_phone, '')LIKE CONCAT('%', :kw, '%')
              )
            """,
            nativeQuery = true
    )
    Page<Map<String, Object>> adminPage(@Param("kw") String keyword, Pageable pageable);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM orders WHERE id IN (:ids)", nativeQuery = true)
    int deleteByIds(@Param("ids") List<Long> ids);

    @Transactional
    @Modifying
    @Query(value = """
    UPDATE orders
    SET status = :status,
        cancelled_at = CASE WHEN :status = 'CANCELLED' THEN NOW() ELSE cancelled_at END,
        finished_at  = CASE WHEN :status = 'FINISHED' THEN NOW() ELSE finished_at END,
        paid_at      = CASE WHEN :status = 'PAID' THEN NOW() ELSE paid_at END
    WHERE id = :id
    """, nativeQuery = true)
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Transactional
    @Modifying
    @Query(value = """
    UPDATE orders
    SET status = 'PAID',
        pay_type = :payType,
        paid_at = NOW()
    WHERE id = :id
      AND status = 'UNPAID'
    """, nativeQuery = true)
    int updatePaidInfo(@Param("id") Long id, @Param("payType") String payType);

    @Transactional
    @Modifying
    @Query(value = """
    UPDATE orders
    SET status = 'FINISHED',
        finished_at = NOW()
    WHERE buyer_id = :uid
      AND status = 'PAID'
      AND paid_at IS NOT NULL
      AND paid_at <= :deadline
    """, nativeQuery = true)
    int autoFinishExpiredPaidOrders(@Param("uid") Long userId, @Param("deadline") LocalDateTime deadline);

    @Query(
            value = """
    SELECT
      o.id AS id,
      o.order_no AS orderNo,
      o.product_id AS productId,
      o.product_title AS productTitle,
      o.amount AS amount,
      o.status AS status,
      o.buyer_id AS buyerId,
      COALESCE(NULLIF(TRIM(bu.nick_name), ''), NULLIF(TRIM(bu.username), ''), '校园用户') AS buyerName,
      COALESCE(bu.avatar, '') AS buyerAvatar,
      o.seller_id AS sellerId,
      COALESCE(NULLIF(TRIM(su.nick_name), ''), NULLIF(TRIM(su.username), ''), '校园用户') AS sellerName,
      COALESCE(su.avatar, '') AS sellerAvatar,
      o.address_id AS addressId,
      COALESCE(NULLIF(TRIM(o.contact_name), ''), ua.contact_name, '-') AS receiver,
      COALESCE(NULLIF(TRIM(o.contact_phone), ''), ua.contact_phone, '-') AS phone,
      COALESCE(NULLIF(TRIM(o.address_text), ''), ua.address_text, '-') AS receiveAddress,
      COALESCE(NULLIF(TRIM(o.trade_location), ''), NULLIF(TRIM(p.address_text), ''), '-') AS tradeLocation,
      o.created_at AS createdAt,
      o.paid_at AS paidAt,
      o.pay_type AS payType,
      o.cancelled_at AS cancelledAt,
      o.finished_at AS finishedAt,
      (
        SELECT pi.url
        FROM product_image pi
        WHERE pi.product_id = o.product_id
        ORDER BY pi.sort ASC, pi.id ASC
        LIMIT 1
      ) AS coverUrl
    FROM orders o
    LEFT JOIN product p ON p.id = o.product_id
    LEFT JOIN sys_user bu ON bu.id = o.buyer_id
    LEFT JOIN sys_user su ON su.id = o.seller_id
    LEFT JOIN user_address ua ON ua.id =
      COALESCE(
        o.address_id,
        (SELECT ua2.id
         FROM user_address ua2
         WHERE ua2.user_id = o.buyer_id AND ua2.status = 1
         ORDER BY ua2.is_default DESC, ua2.id DESC
         LIMIT 1)
      )
    WHERE
      (:status IS NULL OR :status = '' OR o.status = :status)
      AND
      (:payType IS NULL OR :payType = '' OR o.pay_type = :payType)
      AND
      (:kw IS NULL OR :kw = '' OR
        COALESCE(o.order_no,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.product_title,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(bu.username,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(bu.nick_name,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(su.username,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(su.nick_name,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.address_text, ua.address_text, '') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.trade_location,'') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(p.address_text,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.contact_phone, ua.contact_phone, '') LIKE CONCAT('%', :kw, '%')
      )
    ORDER BY o.created_at DESC
  """,
            countQuery = """
    SELECT COUNT(1)
    FROM orders o
    LEFT JOIN product p ON p.id = o.product_id
    LEFT JOIN sys_user bu ON bu.id = o.buyer_id
    LEFT JOIN sys_user su ON su.id = o.seller_id
    LEFT JOIN user_address ua ON ua.id =
      COALESCE(
        o.address_id,
        (SELECT ua2.id
         FROM user_address ua2
         WHERE ua2.user_id = o.buyer_id AND ua2.status = 1
         ORDER BY ua2.is_default DESC, ua2.id DESC
         LIMIT 1)
      )
    WHERE
      (:status IS NULL OR :status = '' OR o.status = :status)
      AND
      (:payType IS NULL OR :payType = '' OR o.pay_type = :payType)
      AND
      (:kw IS NULL OR :kw = '' OR
        COALESCE(o.order_no,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.product_title,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(bu.username,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(bu.nick_name,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(su.username,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(su.nick_name,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.address_text, ua.address_text, '') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.trade_location,'') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(p.address_text,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.contact_phone, ua.contact_phone, '') LIKE CONCAT('%', :kw, '%')
      )
  """,
            nativeQuery = true
    )
    Page<Map<String, Object>> adminPageWithStatus(@Param("kw") String keyword,
                                                  @Param("status") String status,
                                                  @Param("payType") String payType,
                                                  Pageable pageable);

    @Query(
            value = """
    SELECT
      COALESCE(o.pay_type, 'UNSET') AS payType,
      COUNT(1) AS cnt
    FROM orders o
    LEFT JOIN product p ON p.id = o.product_id
    LEFT JOIN sys_user bu ON bu.id = o.buyer_id
    LEFT JOIN sys_user su ON su.id = o.seller_id
    LEFT JOIN user_address ua ON ua.id =
      COALESCE(
        o.address_id,
        (SELECT ua2.id
         FROM user_address ua2
         WHERE ua2.user_id = o.buyer_id AND ua2.status = 1
         ORDER BY ua2.is_default DESC, ua2.id DESC
         LIMIT 1)
      )
    WHERE
      (:status IS NULL OR :status = '' OR o.status = :status)
      AND
      (:payType IS NULL OR :payType = '' OR o.pay_type = :payType)
      AND
      (:kw IS NULL OR :kw = '' OR
        COALESCE(o.order_no,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.product_title,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(bu.username,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(bu.nick_name,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(su.username,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(su.nick_name,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.address_text, ua.address_text, '') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.trade_location,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(p.address_text,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.contact_phone, ua.contact_phone, '') LIKE CONCAT('%', :kw, '%')
      )
    GROUP BY COALESCE(o.pay_type, 'UNSET')
  """,
            nativeQuery = true
    )
    List<Map<String, Object>> adminPayTypeStats(@Param("kw") String keyword,
                                                @Param("status") String status,
                                                @Param("payType") String payType);

    @Query(
            value = """
    SELECT
      DATE_FORMAT(days.day_date, '%m-%d') AS label,
      COUNT(o.id) AS value
    FROM (
      SELECT CURDATE() - INTERVAL 6 DAY AS day_date
      UNION ALL SELECT CURDATE() - INTERVAL 5 DAY
      UNION ALL SELECT CURDATE() - INTERVAL 4 DAY
      UNION ALL SELECT CURDATE() - INTERVAL 3 DAY
      UNION ALL SELECT CURDATE() - INTERVAL 2 DAY
      UNION ALL SELECT CURDATE() - INTERVAL 1 DAY
      UNION ALL SELECT CURDATE()
    ) days
    LEFT JOIN orders o
      ON DATE(COALESCE(o.paid_at, o.created_at)) = days.day_date
     AND o.status IN ('PAID', 'FINISHED')
    GROUP BY days.day_date
    ORDER BY days.day_date
    """,
            nativeQuery = true
    )
    List<Map<String, Object>> adminDailySalesTrend();

    @Query(
            value = """
    SELECT
      o.status AS label,
      COUNT(1) AS value
    FROM orders o
    GROUP BY o.status
    """,
            nativeQuery = true
    )
    List<Map<String, Object>> adminOrderStatusStats();

    @Query(
            value = """
    SELECT
      COALESCE(NULLIF(TRIM(COALESCE(su.nick_name, '')), ''), su.username, CONCAT('seller-', o.seller_id)) AS label,
      COALESCE(ROUND(SUM(o.amount), 2), 0) AS value
    FROM orders o
    LEFT JOIN product p ON p.id = o.product_id
    LEFT JOIN sys_user su ON su.id = o.seller_id
    WHERE o.status IN ('PAID', 'FINISHED')
    GROUP BY o.seller_id, COALESCE(NULLIF(TRIM(COALESCE(su.nick_name, '')), ''), su.username, CONCAT('seller-', o.seller_id))
    ORDER BY SUM(o.amount) DESC
    LIMIT 7
    """,
            nativeQuery = true
    )
    List<Map<String, Object>> adminSellerAmountStats();

    Page<Orders> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);
    Page<Orders> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    List<Orders> findByBuyerIdAndStatusAndCreatedAtLessThanEqual(Long buyerId, String status, LocalDateTime deadline);
    List<Orders> findBySellerIdAndStatusAndCreatedAtLessThanEqual(Long sellerId, String status, LocalDateTime deadline);
    List<Orders> findByStatusAndPaidAtIsNotNullAndPaidAtLessThanEqualAndFinishedAtIsNull(String status, LocalDateTime deadline);
    List<Orders> findByBuyerIdAndStatusAndPaidAtIsNotNullAndPaidAtLessThanEqualAndFinishedAtIsNull(Long buyerId, String status, LocalDateTime deadline);

//    閻劍鍩涢幋鎴犳畱鐠併垹宕熼崚鍡涖€?
@Query(
        value = """
    SELECT
      o.id AS id,
      o.order_no AS orderNo,
      o.product_id AS productId,
      o.product_title AS productTitle,
      o.amount AS amount,
      o.status AS status,
      o.buyer_id AS buyerId,
      COALESCE(NULLIF(TRIM(bu.nick_name), ''), NULLIF(TRIM(bu.username), ''), '校园用户') AS buyerName,
      COALESCE(bu.avatar, '') AS buyerAvatar,
      o.seller_id AS sellerId,
      COALESCE(NULLIF(TRIM(su.nick_name), ''), NULLIF(TRIM(su.username), ''), '校园用户') AS sellerName,
      COALESCE(su.avatar, '') AS sellerAvatar,
      o.address_id AS addressId,
      COALESCE(NULLIF(TRIM(o.contact_name), ''), ua.contact_name, '-') AS receiver,
      COALESCE(NULLIF(TRIM(o.contact_phone), ''), ua.contact_phone, '-') AS phone,
      COALESCE(NULLIF(TRIM(o.address_text), ''), ua.address_text, '-') AS receiveAddress,
      COALESCE(NULLIF(TRIM(o.trade_location), ''), NULLIF(TRIM(p.address_text), ''), '-') AS tradeLocation,
      o.created_at AS createdAt,
      o.paid_at AS paidAt,
      o.pay_type AS payType,
      o.cancelled_at AS cancelledAt,
      o.finished_at AS finishedAt,
      (
        SELECT pi.url
        FROM product_image pi
        WHERE pi.product_id = o.product_id
        ORDER BY pi.sort ASC, pi.id ASC
        LIMIT 1
      ) AS coverUrl
    FROM orders o
    LEFT JOIN product p ON p.id = o.product_id
    LEFT JOIN sys_user bu ON bu.id = o.buyer_id
    LEFT JOIN sys_user su ON su.id = o.seller_id
    LEFT JOIN user_address ua ON ua.id =
      COALESCE(
        o.address_id,
        (SELECT ua2.id
         FROM user_address ua2
         WHERE ua2.user_id = o.buyer_id AND ua2.status = 1
         ORDER BY ua2.is_default DESC, ua2.id DESC
         LIMIT 1)
      )
    WHERE o.buyer_id = :uid
      AND COALESCE(o.buyer_visible, 1) = 1
      AND (:status IS NULL OR :status = '' OR o.status = :status)
      AND (:payType IS NULL OR :payType = '' OR o.pay_type = :payType)
      AND (:kw IS NULL OR :kw = '' OR
        COALESCE(o.order_no,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.product_title,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(su.username,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(su.nick_name,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.address_text, ua.address_text, '') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.trade_location,'') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(p.address_text,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.contact_phone, ua.contact_phone, '')LIKE CONCAT('%', :kw, '%')
      )
    ORDER BY o.created_at DESC
  """,
        countQuery = """
    SELECT COUNT(1)
    FROM orders o
    LEFT JOIN product p ON p.id = o.product_id
    LEFT JOIN sys_user su ON su.id = o.seller_id
    LEFT JOIN user_address ua ON ua.id =
      COALESCE(
        o.address_id,
        (SELECT ua2.id
         FROM user_address ua2
         WHERE ua2.user_id = o.buyer_id AND ua2.status = 1
         ORDER BY ua2.is_default DESC, ua2.id DESC
         LIMIT 1)
      )
    WHERE o.buyer_id = :uid
      AND COALESCE(o.buyer_visible, 1) = 1
      AND (:status IS NULL OR :status = '' OR o.status = :status)
      AND (:payType IS NULL OR :payType = '' OR o.pay_type = :payType)
      AND (:kw IS NULL OR :kw = '' OR
        COALESCE(o.order_no,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.product_title,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(su.username,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(su.nick_name,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.address_text, ua.address_text, '') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.trade_location,'') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(p.address_text,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.contact_phone, ua.contact_phone, '')LIKE CONCAT('%', :kw, '%')
      )
  """,
        nativeQuery = true
)
Page<Map<String, Object>> userBuyerPage(
        @Param("uid") Long userId,
        @Param("kw") String keyword,
        @Param("status") String status,
        @Param("payType") String payType,
        Pageable pageable
);

    @Query(
            value = """
    SELECT
      o.id AS id,
      o.order_no AS orderNo,
      o.product_id AS productId,
      o.product_title AS productTitle,
      o.amount AS amount,
      o.status AS status,
      o.buyer_id AS buyerId,
      COALESCE(NULLIF(TRIM(bu.nick_name), ''), NULLIF(TRIM(bu.username), ''), '校园用户') AS buyerName,
      COALESCE(bu.avatar, '') AS buyerAvatar,
      o.seller_id AS sellerId,
      COALESCE(NULLIF(TRIM(su.nick_name), ''), NULLIF(TRIM(su.username), ''), '校园用户') AS sellerName,
      COALESCE(su.avatar, '') AS sellerAvatar,
      o.address_id AS addressId,
      COALESCE(NULLIF(TRIM(o.contact_name), ''), ua.contact_name, '-') AS receiver,
      COALESCE(NULLIF(TRIM(o.contact_phone), ''), ua.contact_phone, '-') AS phone,
      COALESCE(NULLIF(TRIM(o.address_text), ''), ua.address_text, '-') AS receiveAddress,
      COALESCE(NULLIF(TRIM(o.trade_location), ''), NULLIF(TRIM(p.address_text), ''), '-') AS tradeLocation,
      o.created_at AS createdAt,
      o.paid_at AS paidAt,
      o.pay_type AS payType,
      o.cancelled_at AS cancelledAt,
      o.finished_at AS finishedAt,
      (
        SELECT pi.url
        FROM product_image pi
        WHERE pi.product_id = o.product_id
        ORDER BY pi.sort ASC, pi.id ASC
        LIMIT 1
      ) AS coverUrl
    FROM orders o
    LEFT JOIN product p ON p.id = o.product_id
    LEFT JOIN sys_user bu ON bu.id = o.buyer_id
    LEFT JOIN sys_user su ON su.id = o.seller_id
    LEFT JOIN user_address ua ON ua.id =
      COALESCE(
        o.address_id,
        (SELECT ua2.id
         FROM user_address ua2
         WHERE ua2.user_id = o.buyer_id AND ua2.status = 1
         ORDER BY ua2.is_default DESC, ua2.id DESC
         LIMIT 1)
      )
    WHERE o.seller_id = :uid
      AND COALESCE(o.seller_visible, 1) = 1
      AND (:status IS NULL OR :status = '' OR o.status = :status)
      AND (:payType IS NULL OR :payType = '' OR o.pay_type = :payType)
      AND (:kw IS NULL OR :kw = '' OR
        COALESCE(o.order_no,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.product_title,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(bu.username,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(bu.nick_name,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.address_text, ua.address_text, '') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.trade_location,'') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(p.address_text,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.contact_phone, ua.contact_phone, '')LIKE CONCAT('%', :kw, '%')
      )
    ORDER BY o.created_at DESC
  """,
            countQuery = """
    SELECT COUNT(1)
    FROM orders o
    LEFT JOIN product p ON p.id = o.product_id
    LEFT JOIN sys_user bu ON bu.id = o.buyer_id
    LEFT JOIN user_address ua ON ua.id =
      COALESCE(
        o.address_id,
        (SELECT ua2.id
         FROM user_address ua2
         WHERE ua2.user_id = o.buyer_id AND ua2.status = 1
         ORDER BY ua2.is_default DESC, ua2.id DESC
         LIMIT 1)
      )
    WHERE o.seller_id = :uid
      AND COALESCE(o.seller_visible, 1) = 1
      AND (:status IS NULL OR :status = '' OR o.status = :status)
      AND (:payType IS NULL OR :payType = '' OR o.pay_type = :payType)
      AND (:kw IS NULL OR :kw = '' OR
        COALESCE(o.order_no,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.product_title,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(bu.username,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(bu.nick_name,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.address_text, ua.address_text, '') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(o.trade_location,'') LIKE CONCAT('%', :kw, '%') OR
                COALESCE(p.address_text,'') LIKE CONCAT('%', :kw, '%') OR
        COALESCE(o.contact_phone, ua.contact_phone, '')LIKE CONCAT('%', :kw, '%')
      )
  """,
            nativeQuery = true
    )
    Page<Map<String, Object>> userSellerPage(
            @Param("uid") Long userId,
            @Param("kw") String keyword,
            @Param("status") String status,
            @Param("payType") String payType,
            Pageable pageable
    );

    @Query(
            value = """
    SELECT
      cl.id AS id,
      cl.user_id AS userId,
      cl.change_val AS changeVal,
      cl.reason AS reason,
      cl.biz_type AS bizType,
      cl.biz_id AS bizId,
      cl.remark AS remark,
      cl.created_at AS createdAt,
      o.buyer_rate AS buyerRate,
      o.buyer_comment AS buyerComment,
      o.reviewed_at AS reviewedAt,
      o.buyer_id AS sourceUserId,
      COALESCE(bu.username, '') AS sourceUserName,
      COALESCE(bu.nick_name, '') AS sourceNickName,
      COALESCE(bu.avatar, '') AS sourceAvatar
    FROM credit_log cl
    INNER JOIN orders o ON o.id = cl.biz_id
    LEFT JOIN sys_user bu ON bu.id = o.buyer_id
    WHERE cl.user_id = :sellerId
      AND o.seller_id = :sellerId
      AND o.status = 'FINISHED'
      AND cl.biz_type = 'orders'
      AND cl.reason IN ('ORDER_FINISH', 'ORDER_FINISHED')
    ORDER BY cl.id DESC
  """,
            countQuery = """
    SELECT COUNT(1)
    FROM credit_log cl
    INNER JOIN orders o ON o.id = cl.biz_id
    WHERE cl.user_id = :sellerId
      AND o.seller_id = :sellerId
      AND o.status = 'FINISHED'
      AND cl.biz_type = 'orders'
      AND cl.reason IN ('ORDER_FINISH', 'ORDER_FINISHED')
  """,
            nativeQuery = true
    )
    Page<Map<String, Object>> sellerFinishedReviewPage(@Param("sellerId") Long sellerId, Pageable pageable);
}



