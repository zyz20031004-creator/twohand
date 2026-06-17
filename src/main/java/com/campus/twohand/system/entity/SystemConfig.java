package com.campus.twohand.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "system_config")
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_name", nullable = false, length = 100)
    private String siteName;

    @Column(name = "site_subtitle", length = 255)
    private String siteSubtitle;

    @Column(name = "notice_title", length = 100)
    private String noticeTitle;

    @Column(name = "notice_content", columnDefinition = "TEXT")
    private String noticeContent;

    @Column(name = "product_audit_enabled", nullable = false)
    private Boolean productAuditEnabled;

    @Column(name = "max_upload_count", nullable = false)
    private Integer maxUploadCount;

    @Column(name = "contact_info", length = 255)
    private String contactInfo;

    @Column(name = "site_desc", columnDefinition = "TEXT")
    private String siteDesc;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
