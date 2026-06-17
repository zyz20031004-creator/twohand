package com.campus.twohand.user.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 管理员操作审计日志实体
 * <p>
 * 映射到 `admin_audit_log` 表，记录管理员对系统进行的敏感操作，
 * 例如启用/禁用用户等，以便于追踪和审计。
 */
@Entity
@Table(name = "admin_audit_log")
public class AdminAuditLog {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 执行操作的管理员ID
     */
    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    /**
     * 被操作的目标用户ID（如果适用）
     */
    @Column(name = "target_user_id")
    private Long targetUserId;

    /**
     * 操作类型 (如 'ENABLE', 'DISABLE')
     */
    @Column(nullable = false, length = 20)
    private String action;

    /**
     * 操作前的数据快照（JSON格式）
     */
    @Lob
    @Column(name = "before_data")
    private String beforeData;

    /**
     * 操作后的数据快照（JSON格式）
     */
    @Lob
    @Column(name = "after_data")
    private String afterData;

    /**
     * 操作员的IP地址
     */
    @Column(length = 64)
    private String ip;

    /**
     * 记录创建时间（由数据库自动生成）
     */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== getter / setter =====

    public Long getId() {
        return id;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getBeforeData() {
        return beforeData;
    }

    public void setBeforeData(String beforeData) {
        this.beforeData = beforeData;
    }

    public String getAfterData() {
        return afterData;
    }

    public void setAfterData(String afterData) {
        this.afterData = afterData;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
