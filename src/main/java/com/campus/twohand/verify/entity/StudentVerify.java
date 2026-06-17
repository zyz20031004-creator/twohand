package com.campus.twohand.verify.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "student_verify")
public class StudentVerify {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String school;

    @Column(name = "student_no", nullable = false, length = 32)
    private String studentNo;

    @Column(name = "real_name", length = 64)
    private String realName;

    @Column(name = "proof_url", length = 255)
    private String proofUrl;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
