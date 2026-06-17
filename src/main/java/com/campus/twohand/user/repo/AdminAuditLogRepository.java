package com.campus.twohand.user.repo;

import com.campus.twohand.user.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 管理员操作审计日志表（AdminAuditLog）的JPA仓库接口
 */
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
}
