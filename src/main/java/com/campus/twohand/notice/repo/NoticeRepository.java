package com.campus.twohand.notice.repo;

import com.campus.twohand.notice.entity.Notice;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // ✅ 只展示上架(status=1) 的公告，按时间倒序
    @Query("select n from Notice n where n.status = 1 order by n.createdAt desc")
    Page<Notice> pageOnline(Pageable pageable);

//    管理员
    @Query(value = """
        SELECT
          n.id AS id,
          n.title AS title,
          n.content AS content,
          n.status AS status,
          n.creator_id AS creatorId,
          COALESCE(u.nick_name, u.username) AS creatorName,
          DATE_FORMAT(n.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
        FROM notice n
        LEFT JOIN sys_user u ON u.id = n.creator_id
        WHERE 1=1
          AND (:status IS NULL OR n.status = :status)
          AND (:keyword IS NULL OR :keyword = '' OR n.title LIKE CONCAT('%', :keyword, '%'))
        ORDER BY n.created_at DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<Map<String, Object>> adminPage(@Param("keyword") String keyword,
                                        @Param("status") Integer status,
                                        @Param("size") int size,
                                        @Param("offset") int offset);

    @Query(value = """
        SELECT COUNT(1)
        FROM notice n
        WHERE 1=1
          AND (:status IS NULL OR n.status = :status)
          AND (:keyword IS NULL OR :keyword = '' OR n.title LIKE CONCAT('%', :keyword, '%'))
        """, nativeQuery = true)
    long adminCount(@Param("keyword") String keyword,
                    @Param("status") Integer status);

    @Modifying
    @Query(value = """
        INSERT INTO notice(title, content, creator_id, status, created_at, updated_at)
        VALUES (:title, :content, :creatorId, :status, NOW(), NOW())
        """, nativeQuery = true)
    int adminInsert(@Param("title") String title,
                    @Param("content") String content,
                    @Param("creatorId") Long creatorId,
                    @Param("status") Integer status);

    @Modifying
    @Query(value = """
        UPDATE notice
        SET title = :title,
            content = :content,
            status = :status,
            updated_at = NOW()
        WHERE id = :id
        """, nativeQuery = true)
    int adminUpdate(@Param("id") Long id,
                    @Param("title") String title,
                    @Param("content") String content,
                    @Param("status") Integer status);

    @Modifying
    @Query(value = "UPDATE notice SET status = :status, updated_at = NOW() WHERE id = :id", nativeQuery = true)
    int adminSetStatus(@Param("id") Long id, @Param("status") Integer status);

    @Modifying
    @Query(value = "DELETE FROM notice WHERE id = :id", nativeQuery = true)
    int adminDeleteById(@Param("id") Long id);

    @Modifying
    @Query(value = "DELETE FROM notice WHERE id IN (:ids)", nativeQuery = true)
    int adminDeleteBatch(@Param("ids") List<Long> ids);
}
