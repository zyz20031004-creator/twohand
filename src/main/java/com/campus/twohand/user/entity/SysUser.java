package com.campus.twohand.user.entity;

import jakarta.persistence.*;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 * <p>
 * 映射到数据库的 `sys_user` 表，存储所有用户（包括普通用户和管理员）的基础信息。
 */
@Entity
@Table(name = "sys_user")
public class SysUser {

    /**
     * 主键ID
     */
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 登录用户名，唯一
     */
    @Setter
    @Column(nullable = false, length = 50, unique = true)
    private String username;

    /**
     * 加密后的密码哈希
     */
    @Setter
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /**
     * 用户角色 (如 'USER', 'ADMIN')
     */
    @Setter
    @Column(nullable = false, length = 20)
    private String role = "USER";

    /**
     * 账户状态 (1: 正常, 0: 禁用)
     */
    @Setter
    @Column(nullable = false)
    private Integer status = 1;

    /**
     * 用户昵称
     */
    @Setter
    @Column(name = "nick_name", length = 50)
    private String name;

    /**
     * 手机号码
     */
    @Setter
    @Column(length = 20)
    private String phone;

    /**
     * 电子邮箱
     */
    @Setter
    @Column(length = 100)
    private String email;

    /**
     * 用户头像URL
     */
    @Setter
    @Column(length = 255)
    private String avatar;

    /**
     * 学号（用于学生认证）
     */
    @Setter
    @Column(name = "student_no", length = 32)
    private String studentNo;

    /**
     * 学校名称
     */
    @Setter
    @Column(length = 64)
    private String school;

    /**
     * 真实姓名（用于学生认证）
     */
    @Setter
    @Column(name = "real_name", length = 64)
    private String realName;

    /**
     * 学生认证状态 (如 'UNVERIFIED', 'VERIFIED', 'FAILED')
     */
    @Setter
    @Column(name = "verify_status", nullable = false, length = 16)
    private String verifyStatus = "UNVERIFIED";

    /**
     * 认证通过或失败的时间
     */
    @Setter
    @Column(name = "verify_time")
    private LocalDateTime verifyTime;

    /**
     * 用户信誉分
     */
    @Setter
    @Column(name = "credit_score", nullable = false)
    private Integer creditScore = 100;

    /**
     * 记录创建时间（由数据库自动生成）
     */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 记录更新时间（由数据库自动生成）
     */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // ===== getter / setter =====

    public Long getId() { return id; }

    public String getUsername() { return username; }

    public String getPasswordHash() { return passwordHash; }

    public String getRole() { return role; }

    public Integer getStatus() { return status; }

    public String getName() { return name; }

    public String getPhone() { return phone; }

    public String getEmail() { return email; }

    public String getAvatar() { return avatar; }

    public String getStudentNo() { return studentNo; }

    public String getSchool() { return school; }

    public String getRealName() { return realName; }

    public String getVerifyStatus() { return verifyStatus; }

    public LocalDateTime getVerifyTime() { return verifyTime; }

    public Integer getCreditScore() { return creditScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
