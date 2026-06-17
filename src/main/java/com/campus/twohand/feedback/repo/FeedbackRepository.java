package com.campus.twohand.feedback.repo;

import com.campus.twohand.feedback.entity.Feedback;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    long countByStatus(String status);

    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 管理员：分页列表（keyword：主题/内容/联系方式/邮箱） */
    @Query(value = """
        SELECT
          f.id AS id,
          f.subject AS subject,
          f.content AS content,
          f.contact AS phone,
          f.email AS email,
          f.reply AS reply,
          f.status AS status,
          DATE_FORMAT(f.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt,
          DATE_FORMAT(f.replied_at, '%Y-%m-%d %H:%i:%s') AS repliedAt,
          f.replied_by AS repliedBy
        FROM feedback f
        WHERE (
          :keyword IS NULL OR :keyword = '' OR
          f.subject LIKE CONCAT('%', :keyword, '%') OR
          f.content LIKE CONCAT('%', :keyword, '%') OR
          f.contact LIKE CONCAT('%', :keyword, '%') OR
          f.email LIKE CONCAT('%', :keyword, '%')
        )
        ORDER BY f.created_at DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<Map<String, Object>> adminPage(@Param("keyword") String keyword,
                                        @Param("size") int size,
                                        @Param("offset") int offset);

    @Query(value = """
        SELECT COUNT(1)
        FROM feedback f
        WHERE (
          :keyword IS NULL OR :keyword = '' OR
          f.subject LIKE CONCAT('%', :keyword, '%') OR
          f.content LIKE CONCAT('%', :keyword, '%') OR
          f.contact LIKE CONCAT('%', :keyword, '%') OR
          f.email LIKE CONCAT('%', :keyword, '%')
        )
        """, nativeQuery = true)
    long adminCount(@Param("keyword") String keyword);

    /** 管理员：回复（同时把状态置为 CLOSED） */
    @Modifying
    @Query(value = """
        UPDATE feedback
        SET reply = :reply,
            replied_by = :repliedBy,
            replied_at = NOW(),
            status = 'CLOSED'
        WHERE id = :id
        """, nativeQuery = true)
    int adminReply(@Param("id") Long id,
                   @Param("reply") String reply,
                   @Param("repliedBy") Long repliedBy);

    /** 管理员：批量删除 */
    @Modifying
    @Query(value = "DELETE FROM feedback WHERE id IN (:ids)", nativeQuery = true)
    int adminDeleteBatch(@Param("ids") List<Long> ids);

    /**
     * 用户端：获取我的反馈列表
     */
    @Query(value = """
        select
          f.id, f.subject, f.content, f.contact, f.email, f.status, f.reply, f.created_at
        from feedback f
        where f.user_id = :userId
          and (:status is null or :status = '' or f.status = :status)
          and (:keyword is null or :keyword = '' or f.subject like concat('%', :keyword, '%') or f.content like concat('%', :keyword, '%'))
        order by f.created_at desc
        """,
            countQuery = """
        select count(1)
        from feedback f
        where f.user_id = :userId
          and (:status is null or :status = '' or f.status = :status)
          and (:keyword is null or :keyword = '' or f.subject like concat('%', :keyword, '%') or f.content like concat('%', :keyword, '%'))
        """,
            nativeQuery = true)
    Page<Object[]> pageMyFeedback(@Param("userId") Long userId,
                                  @Param("status") String status,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);
}
