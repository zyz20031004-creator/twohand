package com.campus.twohand.verify.repo;

import com.campus.twohand.verify.entity.StudentVerify;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Map;

public interface StudentVerifyRepository extends JpaRepository<StudentVerify, Long> {

    long countByStatus(String status);

    StudentVerify findTopByUserIdOrderByCreatedAtDesc(Long userId);

    StudentVerify findTopBySchoolAndStudentNoOrderByCreatedAtDesc(String school, String studentNo);

    boolean existsBySchoolAndStudentNo(String school, String studentNo);

    boolean existsBySchoolAndStudentNoAndUserIdNotAndStatusIn(String school,
                                                              String studentNo,
                                                              Long userId,
                                                              Collection<String> statuses);

    boolean existsByUserIdAndStatus(Long userId, String status);

    @Query(value = """
            SELECT
              sv.id AS id,
              sv.user_id AS userId,
              sv.school AS school,
              sv.student_no AS studentNo,
              sv.real_name AS realName,
              sv.proof_url AS proofUrl,
              sv.status AS status,
              sv.reject_reason AS rejectReason,
              sv.created_at AS createdAt,
              u.username AS username
            FROM student_verify sv
            LEFT JOIN sys_user u ON u.id = sv.user_id
            WHERE
              (:status IS NULL OR :status = '' OR sv.status = :status)
              AND
              (:keyword IS NULL OR :keyword = '' OR
                COALESCE(sv.school, '') LIKE CONCAT('%', :keyword, '%') OR
                COALESCE(sv.student_no, '') LIKE CONCAT('%', :keyword, '%') OR
                COALESCE(sv.real_name, '') LIKE CONCAT('%', :keyword, '%') OR
                COALESCE(u.username, '') LIKE CONCAT('%', :keyword, '%') OR
                COALESCE(u.nick_name, '') LIKE CONCAT('%', :keyword, '%')
              )
            ORDER BY sv.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(1)
            FROM student_verify sv
            LEFT JOIN sys_user u ON u.id = sv.user_id
            WHERE
              (:status IS NULL OR :status = '' OR sv.status = :status)
              AND
              (:keyword IS NULL OR :keyword = '' OR
                COALESCE(sv.school, '') LIKE CONCAT('%', :keyword, '%') OR
                COALESCE(sv.student_no, '') LIKE CONCAT('%', :keyword, '%') OR
                COALESCE(sv.real_name, '') LIKE CONCAT('%', :keyword, '%') OR
                COALESCE(u.username, '') LIKE CONCAT('%', :keyword, '%') OR
                COALESCE(u.nick_name, '') LIKE CONCAT('%', :keyword, '%')
              )
            """,
            nativeQuery = true)
    Page<Map<String, Object>> adminPage(@Param("status") String status,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);
}
