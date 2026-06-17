package com.campus.twohand.report.repo;

import com.campus.twohand.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface ReportRepository extends JpaRepository<Report, Long> {

    long countByStatus(String status);

    boolean existsByReporterIdAndProductIdAndStatus(
            Long reporterId,
            Long productId,
            String status
    );

    boolean existsByProductId(Long productId);

    @Query(value = """
        SELECT
          r.id AS id,
          r.reporter_id AS reporterId,
          r.product_id AS productId,
          r.reason AS reason,
          r.detail AS detail,
          r.status AS status,
          r.handle_remark AS handleRemark,
          r.handled_by AS handledBy,
          DATE_FORMAT(r.handled_at, '%Y-%m-%d %H:%i:%s') AS handledAt,
          DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt,
          COALESCE(NULLIF(TRIM(ru.nick_name), ''), NULLIF(TRIM(ru.username), ''), CONCAT('用户', r.reporter_id)) AS reporterName,
          ru.nick_name AS reporterNickName,
          ru.username AS reporterUsername,
          COALESCE(NULLIF(TRIM(hu.nick_name), ''), NULLIF(TRIM(hu.username), ''), CONCAT('管理员', r.handled_by)) AS handlerName,
          hu.nick_name AS handlerNickName,
          hu.username AS handlerUsername,
          p.title AS productTitle
        FROM report r
        LEFT JOIN sys_user ru ON ru.id = r.reporter_id
        LEFT JOIN sys_user hu ON hu.id = r.handled_by
        LEFT JOIN product p ON p.id = r.product_id
        WHERE (:status IS NULL OR :status = '' OR r.status = :status)
          AND (
            :keyword IS NULL OR :keyword = '' OR
            CAST(r.id AS CHAR) LIKE CONCAT('%', :keyword, '%') OR
            CAST(r.product_id AS CHAR) LIKE CONCAT('%', :keyword, '%') OR
            r.reason LIKE CONCAT('%', :keyword, '%') OR
            COALESCE(ru.nick_name, ru.username, '') LIKE CONCAT('%', :keyword, '%') OR
            COALESCE(p.title, '') LIKE CONCAT('%', :keyword, '%')
          )
        ORDER BY
          CASE r.status
            WHEN 'PENDING' THEN 0
            WHEN 'VALID' THEN 1
            WHEN 'INVALID' THEN 2
            ELSE 3
          END,
          r.id DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Map<String, Object>> adminPage(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(1)
        FROM report r
        LEFT JOIN sys_user ru ON ru.id = r.reporter_id
        LEFT JOIN product p ON p.id = r.product_id
        WHERE (:status IS NULL OR :status = '' OR r.status = :status)
          AND (
            :keyword IS NULL OR :keyword = '' OR
            CAST(r.id AS CHAR) LIKE CONCAT('%', :keyword, '%') OR
            CAST(r.product_id AS CHAR) LIKE CONCAT('%', :keyword, '%') OR
            r.reason LIKE CONCAT('%', :keyword, '%') OR
            COALESCE(ru.nick_name, ru.username, '') LIKE CONCAT('%', :keyword, '%') OR
            COALESCE(p.title, '') LIKE CONCAT('%', :keyword, '%')
          )
        """, nativeQuery = true)
    long adminCount(
            @Param("keyword") String keyword,
            @Param("status") String status
    );

    @Query(value = """
        SELECT
          r.id AS id,
          r.product_id AS productId,
          COALESCE(p.title, '') AS productTitle,
          CASE WHEN p.id IS NULL THEN 0 ELSE 1 END AS productExists,
          pi.url AS productCoverUrl,
          r.reason AS reason,
          r.detail AS detail,
          r.status AS status,
          r.handle_remark AS handleRemark,
          DATE_FORMAT(r.handled_at, '%Y-%m-%d %H:%i:%s') AS handledAt,
          DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
        FROM report r
        LEFT JOIN product p ON p.id = r.product_id
        LEFT JOIN product_image pi ON pi.id = (
          SELECT pi2.id
          FROM product_image pi2
          WHERE pi2.product_id = r.product_id
          ORDER BY pi2.sort ASC, pi2.id ASC
          LIMIT 1
        )
        WHERE r.reporter_id = :reporterId
          AND (:status IS NULL OR :status = '' OR r.status = :status)
          AND (
            :keyword IS NULL OR :keyword = '' OR
            CAST(r.id AS CHAR) LIKE CONCAT('%', :keyword, '%') OR
            COALESCE(p.title, '') LIKE CONCAT('%', :keyword, '%') OR
            r.reason LIKE CONCAT('%', :keyword, '%') OR
            COALESCE(r.detail, '') LIKE CONCAT('%', :keyword, '%')
          )
        ORDER BY r.created_at DESC, r.id DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Map<String, Object>> myPage(
            @Param("reporterId") Long reporterId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(1)
        FROM report r
        LEFT JOIN product p ON p.id = r.product_id
        WHERE r.reporter_id = :reporterId
          AND (:status IS NULL OR :status = '' OR r.status = :status)
          AND (
            :keyword IS NULL OR :keyword = '' OR
            CAST(r.id AS CHAR) LIKE CONCAT('%', :keyword, '%') OR
            COALESCE(p.title, '') LIKE CONCAT('%', :keyword, '%') OR
            r.reason LIKE CONCAT('%', :keyword, '%') OR
            COALESCE(r.detail, '') LIKE CONCAT('%', :keyword, '%')
          )
        """, nativeQuery = true)
    long myCount(
            @Param("reporterId") Long reporterId,
            @Param("keyword") String keyword,
            @Param("status") String status
    );
}
