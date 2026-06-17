package com.campus.twohand.comment.repo;

import com.campus.twohand.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    long countByTargetTypeAndTargetIdAndStatus(String targetType, Long targetId, Integer status);

    @Query(value = """
            select
              c.id,
              c.user_id,
              c.content,
              c.created_at,
              coalesce(nullif(trim(u.nick_name), ''), '校园用户') as nickname,
              coalesce(u.avatar, '') as avatar
            from comment c
            left join sys_user u
              on u.id = c.user_id
             and u.role = 'USER'
            where c.target_type = 'WANTED'
              and c.target_id = :targetId
              and c.status = 1
              and c.user_id <> 1
            order by c.created_at desc
            """, nativeQuery = true)
    List<Object[]> listWantedCommentsWithUser(@Param("targetId") Long targetId);

    @Query(value = """
            SELECT
              c.id            AS id,
              c.user_id       AS userId,
              COALESCE(NULLIF(TRIM(u.nick_name), ''), '校园用户') AS nickname,
              COALESCE(u.avatar, '') AS avatar,
              c.parent_id     AS parentId,
              c.content       AS content,
              c.like_count    AS likeCount,
              c.created_at    AS createdAt
            FROM comment c
            LEFT JOIN sys_user u ON u.id = c.user_id
            WHERE c.target_type = 'WANTED'
              AND c.target_id = :targetId
              AND c.status = 1
            ORDER BY c.created_at DESC
            """, nativeQuery = true)
    List<Map<String, Object>> listWantedCommentsWithUserMap(@Param("targetId") Long targetId);

    @Query(value = """
        SELECT
          c.id            AS id,
          c.user_id       AS userId,
          COALESCE(NULLIF(TRIM(u.nick_name), ''), '校园用户') AS nickname,
          COALESCE(NULLIF(TRIM(u.nick_name), ''), '校园用户') AS username,
          COALESCE(u.avatar, '') AS avatar,
          c.parent_id     AS parentId,
          c.content       AS content,
          c.like_count    AS likeCount,
          c.created_at    AS createdAt
        FROM comment c
        LEFT JOIN sys_user u ON u.id = c.user_id
        WHERE c.target_type = 'PRODUCT'
          AND c.target_id = :productId
          AND c.status = 1
        ORDER BY c.created_at DESC
        """, nativeQuery = true)
    List<Map<String, Object>> listProductCommentsWithUser(@Param("productId") Long productId);
}
