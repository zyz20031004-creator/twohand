package com.campus.twohand.wanted.repo;

import com.campus.twohand.wanted.entity.Wanted;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WantedRepository extends JpaRepository<Wanted, Long> {


    @Query(value = """
        select
          w.id, w.user_id, w.title, w.content, w.image_url, w.status, w.view_count, w.created_at,
          coalesce(nullif(trim(u.nick_name), ''), '校园用户') as nickname,
          coalesce(u.avatar, '') as avatar,
          ifnull(cc.comment_count, 0) as comment_count,
          w.school_name as school_name
        from wanted w
        join sys_user u
          on u.id = w.user_id
         and u.role = 'USER'
        left join (
          select
            c.target_id,
            count(1) as comment_count
          from comment c
          where c.target_type = 'WANTED'
            and c.status = 1
          group by c.target_id
        ) cc
          on cc.target_id = w.id
        where (:schoolName is null or w.school_name = :schoolName)
          and (:status is null or w.status = :status)
          and (:keyword is null or :keyword = '' or w.title like concat('%', :keyword, '%') or w.content like concat('%', :keyword, '%'))
        order by w.created_at desc
        """,
            countQuery = """
        select count(1)
        from wanted w
        join sys_user u
          on u.id = w.user_id
         and u.role = 'USER'
        where (:schoolName is null or w.school_name = :schoolName)
          and (:status is null or w.status = :status)
          and (:keyword is null or :keyword = '' or w.title like concat('%', :keyword, '%') or w.content like concat('%', :keyword, '%'))
        """,
            nativeQuery = true)
    Page<Object[]> pageWithUser(@Param("status") String status,
                                @Param("keyword") String keyword,
                                @Param("schoolName") String schoolName,
                                Pageable pageable);

// 管理员
    // =========================
    // Admin: page + count + delete
    // =========================

    @Query(value = """
        SELECT
          w.id AS id,
          w.user_id AS userId,
          COALESCE(u.nick_name, u.username) AS username,
          w.title AS title,
          w.content AS content,
          w.image_url AS imageUrl,
          w.status AS status,
          w.view_count AS viewCount,
          DATE_FORMAT(w.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt,
          w.school_name AS schoolName
        FROM wanted w
        LEFT JOIN sys_user u ON u.id = w.user_id
        WHERE 1=1
          AND (:status IS NULL OR :status = '' OR w.status = :status)
          AND (
            :keyword IS NULL OR :keyword = '' OR
            w.title LIKE CONCAT('%', :keyword, '%') OR
            w.content LIKE CONCAT('%', :keyword, '%') OR
            u.username LIKE CONCAT('%', :keyword, '%') OR
            u.nick_name LIKE CONCAT('%', :keyword, '%')
          )
        ORDER BY w.created_at DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> adminPage(@Param("keyword") String keyword,
                                        @Param("status") String status,
                                        @Param("size") int size,
                                        @Param("offset") int offset);

    @Query(value = """
        SELECT COUNT(1)
        FROM wanted w
        LEFT JOIN sys_user u ON u.id = w.user_id
        WHERE 1=1
          AND (:status IS NULL OR :status = '' OR w.status = :status)
          AND (
            :keyword IS NULL OR :keyword = '' OR
            w.title LIKE CONCAT('%', :keyword, '%') OR
            w.content LIKE CONCAT('%', :keyword, '%') OR
            u.username LIKE CONCAT('%', :keyword, '%') OR
            u.nick_name LIKE CONCAT('%', :keyword, '%')
          )
        """, nativeQuery = true)
    long adminCount(@Param("keyword") String keyword,
                    @Param("status") String status);

    @Modifying
    @Query(value = "DELETE FROM wanted WHERE id = :id", nativeQuery = true)
    int adminDeleteById(@Param("id") Long id);

    @Modifying
    @Query(value = "DELETE FROM wanted WHERE id IN (:ids)", nativeQuery = true)
    int adminDeleteBatch(@Param("ids") List<Long> ids);

    @Modifying
    @Query(value = "UPDATE wanted SET school_name = :schoolName WHERE user_id = :userId", nativeQuery = true)
    int updateSchoolNameByUserId(@Param("userId") Long userId,
                                 @Param("schoolName") String schoolName);

    @Modifying
    @Query(value = "UPDATE wanted SET view_count = view_count + 1 WHERE id = :id", nativeQuery = true)
    int increaseViewCount(@Param("id") Long id);

    /**
     * 用户端：获取我的求购列表
     */
    @Query(value = """
        select
          w.id, w.title, w.content, w.image_url, w.status, w.created_at, w.school_name, w.view_count
        from wanted w
        where w.user_id = :userId
          and (:status is null or :status = '' or w.status = :status)
          and (:keyword is null or :keyword = '' or w.title like concat('%', :keyword, '%') or w.content like concat('%', :keyword, '%'))
        order by w.created_at desc
        """,
            countQuery = """
        select count(1)
        from wanted w
        where w.user_id = :userId
          and (:status is null or :status = '' or w.status = :status)
          and (:keyword is null or :keyword = '' or w.title like concat('%', :keyword, '%') or w.content like concat('%', :keyword, '%'))
        """,
            nativeQuery = true)
    Page<Object[]> pageMyWanted(@Param("userId") Long userId,
                                @Param("status") String status,
                                @Param("keyword") String keyword,
                                Pageable pageable);
}
