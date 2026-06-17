package com.campus.twohand.user.repo;

import com.campus.twohand.user.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统用户表（SysUser）的JPA仓库接口
 */
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    /**
     * 根据角色统计用户数量
     */
    long countByRole(String role);

    /**
     * 根据用户名查找用户
     */
    Optional<SysUser> findByUsername(String username);

    /**
     * 根据手机号查找用户
     */
    Optional<SysUser> findByPhone(String phone);

    /**
     * 根据邮箱查找用户
     */
    Optional<SysUser> findByEmail(String email);

    Optional<SysUser> findFirstByRoleAndStatusAndVerifyStatusOrderByIdAsc(String role, Integer status, String verifyStatus);

    Optional<SysUser> findFirstByRoleAndStatusOrderByIdAsc(String role, Integer status);

    List<SysUser> findByRoleAndStatusOrderByIdAsc(String role, Integer status);

    boolean existsBySchoolAndStudentNoAndIdNot(String school, String studentNo, Long id);

    // ===== 管理员管理 =====

    /**
     * 分页查询管理员列表
     */
    @Query(value = """
        SELECT
          u.id AS id,
          u.username AS username,
          COALESCE(u.nick_name, '') AS name,
          COALESCE(u.phone, '') AS phone,
          COALESCE(u.email, '') AS email,
          COALESCE(u.avatar, '') AS avatar,
          u.status AS status
        FROM sys_user u
        WHERE u.role IN ('ADMIN', 'SUPER_ADMIN')
          AND (
            :keyword IS NULL OR :keyword = '' OR
            u.username LIKE CONCAT('%', :keyword, '%') OR
            u.nick_name LIKE CONCAT('%', :keyword, '%') OR
            u.phone LIKE CONCAT('%', :keyword, '%') OR
            u.email LIKE CONCAT('%', :keyword, '%')
          )
        ORDER BY u.created_at DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<Map<String, Object>> adminPage(@Param("keyword") String keyword,
                                        @Param("size") int size,
                                        @Param("offset") int offset);

    /**
     * 统计符合条件的管理员总数
     * @param keyword 搜索关键词
     * @return 管理员总数
     */
    @Query(value = """
        SELECT COUNT(1)
        FROM sys_user u
        WHERE u.role IN ('ADMIN', 'SUPER_ADMIN')
          AND (
            :keyword IS NULL OR :keyword = '' OR
            u.username LIKE CONCAT('%', :keyword, '%') OR
            u.nick_name LIKE CONCAT('%', :keyword, '%') OR
            u.phone LIKE CONCAT('%', :keyword, '%') OR
            u.email LIKE CONCAT('%', :keyword, '%')
          )
        """, nativeQuery = true)
    long adminCount(@Param("keyword") String keyword);

    /**
     * 批量删除普通管理员
     */
    @Modifying
    @Query(value = "DELETE FROM sys_user WHERE id IN (:ids) AND role = 'ADMIN'", nativeQuery = true)
    int adminDeleteBatch(@Param("ids") List<Long> ids);

    // ===== 普通用户管理 =====

    /**
     * 分页查询普通用户列表
     * @return 用户信息列表
     */
    @Query(value = """
        SELECT
          u.id AS id,
          u.username AS username,
          COALESCE(u.nick_name, '') AS name,
          COALESCE(u.phone, '') AS phone,
          COALESCE(u.email, '') AS email,
          COALESCE(u.avatar, '') AS avatar,
          u.status AS status
        FROM sys_user u
        WHERE u.role = 'USER'
          AND (
            :keyword IS NULL OR :keyword = '' OR
            u.username LIKE CONCAT('%', :keyword, '%') OR
            u.nick_name LIKE CONCAT('%', :keyword, '%') OR
            u.phone LIKE CONCAT('%', :keyword, '%') OR
            u.email LIKE CONCAT('%', :keyword, '%')
          )
        ORDER BY u.created_at DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<Map<String, Object>> userPage(@Param("keyword") String keyword,
                                       @Param("size") int size,
                                       @Param("offset") int offset);

    /**
     * 统计符合条件的普通用户总数
     */
    @Query(value = """
        SELECT COUNT(1)
        FROM sys_user u
        WHERE u.role = 'USER'
          AND (
            :keyword IS NULL OR :keyword = '' OR
            u.username LIKE CONCAT('%', :keyword, '%') OR
            u.nick_name LIKE CONCAT('%', :keyword, '%') OR
            u.phone LIKE CONCAT('%', :keyword, '%') OR
            u.email LIKE CONCAT('%', :keyword, '%')
          )
        """, nativeQuery = true)
    long userCount(@Param("keyword") String keyword);

    /**
     * （信誉管理）分页查询用户列表
     */
    @Query(value = """
        SELECT
          u.id AS id,
          u.username AS username,
          COALESCE(u.nick_name, '') AS name,
          COALESCE(u.real_name, '') AS realName,
          COALESCE(u.school, '') AS school,
          COALESCE(u.verify_status, 'UNVERIFIED') AS verifyStatus,
          COALESCE(u.credit_score, 100) AS creditScore,
          u.status AS status,
          u.updated_at AS updatedAt
        FROM sys_user u
        WHERE u.role = 'USER'
          AND (
            :keyword IS NULL OR :keyword = '' OR
            CAST(u.id AS CHAR) LIKE CONCAT('%', :keyword, '%') OR
            u.username LIKE CONCAT('%', :keyword, '%') OR
            COALESCE(u.nick_name, '') LIKE CONCAT('%', :keyword, '%') OR
            COALESCE(u.real_name, '') LIKE CONCAT('%', :keyword, '%')
          )
          AND (:verifyStatus IS NULL OR :verifyStatus = '' OR u.verify_status = :verifyStatus)
          AND (:minScore IS NULL OR COALESCE(u.credit_score, 100) >= :minScore)
          AND (:maxScore IS NULL OR COALESCE(u.credit_score, 100) <= :maxScore)
        ORDER BY COALESCE(u.credit_score, 100) DESC, u.updated_at DESC, u.id DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<Map<String, Object>> creditUserPage(@Param("keyword") String keyword,
                                             @Param("verifyStatus") String verifyStatus,
                                             @Param("minScore") Integer minScore,
                                             @Param("maxScore") Integer maxScore,
                                             @Param("size") int size,
                                             @Param("offset") int offset);

    /**
     * （信誉管理）统计符合条件的用户总数
     */
    @Query(value = """
        SELECT COUNT(1)
        FROM sys_user u
        WHERE u.role = 'USER'
          AND (
            :keyword IS NULL OR :keyword = '' OR
            CAST(u.id AS CHAR) LIKE CONCAT('%', :keyword, '%') OR
            u.username LIKE CONCAT('%', :keyword, '%') OR
            COALESCE(u.nick_name, '') LIKE CONCAT('%', :keyword, '%') OR
            COALESCE(u.real_name, '') LIKE CONCAT('%', :keyword, '%')
          )
          AND (:verifyStatus IS NULL OR :verifyStatus = '' OR u.verify_status = :verifyStatus)
          AND (:minScore IS NULL OR COALESCE(u.credit_score, 100) >= :minScore)
          AND (:maxScore IS NULL OR COALESCE(u.credit_score, 100) <= :maxScore)
        """, nativeQuery = true)
    long creditUserCount(@Param("keyword") String keyword,
                         @Param("verifyStatus") String verifyStatus,
                         @Param("minScore") Integer minScore,
                         @Param("maxScore") Integer maxScore);

    /**
     * 批量删除普通用户
     */
    @Modifying
    @Query(value = "DELETE FROM sys_user WHERE id IN (:ids) AND role = 'USER'", nativeQuery = true)
    int userDeleteBatch(@Param("ids") List<Long> ids);
}
