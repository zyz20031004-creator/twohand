package com.campus.twohand.address.repo;

import com.campus.twohand.address.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    @Query(value = """
        SELECT
          a.id AS id,
          a.user_id AS userId,
          u.nick_name AS ownerNickName,
          u.username AS ownerUsername,
          a.contact_name AS contactName,
          a.contact_phone AS contactPhone,
          a.address_text AS addressText,
          a.is_default AS isDefault
        FROM user_address a
        LEFT JOIN sys_user u ON u.id = a.user_id
        WHERE a.status = 1
          AND (
            :keyword IS NULL OR :keyword = '' OR
            a.contact_name LIKE CONCAT('%', :keyword, '%') OR
            a.contact_phone LIKE CONCAT('%', :keyword, '%') OR
            a.address_text LIKE CONCAT('%', :keyword, '%') OR
            u.username LIKE CONCAT('%', :keyword, '%') OR
            u.nick_name LIKE CONCAT('%', :keyword, '%') OR
            CAST(a.user_id AS CHAR) LIKE CONCAT('%', :keyword, '%')
          )
        ORDER BY a.updated_at DESC, a.created_at DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<Map<String, Object>> adminPage(@Param("keyword") String keyword,
                                        @Param("size") int size,
                                        @Param("offset") int offset);

    @Query(value = """
        SELECT COUNT(1)
        FROM user_address a
        LEFT JOIN sys_user u ON u.id = a.user_id
        WHERE a.status = 1
          AND (
            :keyword IS NULL OR :keyword = '' OR
            a.contact_name LIKE CONCAT('%', :keyword, '%') OR
            a.contact_phone LIKE CONCAT('%', :keyword, '%') OR
            a.address_text LIKE CONCAT('%', :keyword, '%') OR
            u.username LIKE CONCAT('%', :keyword, '%') OR
            u.nick_name LIKE CONCAT('%', :keyword, '%') OR
            CAST(a.user_id AS CHAR) LIKE CONCAT('%', :keyword, '%')
          )
        """, nativeQuery = true)
    long adminCount(@Param("keyword") String keyword);

    @Modifying
    @Query(value = "UPDATE user_address SET status = 0 WHERE id = :id", nativeQuery = true)
    int adminSoftDelete(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE user_address SET status = 0 WHERE id IN (:ids)", nativeQuery = true)
    int adminSoftDeleteBatch(@Param("ids") List<Long> ids);

    List<UserAddress> findByUserIdAndStatusOrderByIsDefaultDescIdDesc(Long userId, Integer status);

    @Modifying
    @Query(value = "UPDATE user_address SET is_default = 0 WHERE user_id = :uid AND status = 1", nativeQuery = true)
    int clearDefault(@Param("uid") Long uid);
}
