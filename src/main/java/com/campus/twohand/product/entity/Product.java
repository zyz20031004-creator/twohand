package com.campus.twohand.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 * 对应数据库表 product
 */
@Getter
@Setter
@Entity
@Table(name = "product")
public class Product {

    /**
     * 商品主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 发布人ID（对应 sys_user.id）
     */
    @Column(name="seller_id", nullable = false)
    private Long sellerId;

    /**
     * 分类ID（对应 category.id）
     */
    @Column(name="category_id")
    private Long categoryId;

    @Column(name = "school_name", length = 64)
    private String schoolName;

    /**
     * 商品标题
     */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * 商品描述
     * @Lob 表示 TEXT 类型
     */
    @Lob
    private String description;

    /**
     * 商品价格
     * precision=10 表示总共10位
     * scale=2 表示小数2位
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    /**
     * 地址文本（发货/校内位置）
     */
    @Column(name="address_text", length = 255)
    private String addressText;

    /**
     * 上架状态
     * ON = 上架
     * OFF = 下架
     */
    @Column(nullable = false, length = 10)
    private String status = "OFF";

    @Column(name = "sold_flag", nullable = false)
    private Integer soldFlag = 0;

    /**
     * 审核状态
     * PENDING = 待审核
     * APPROVED = 已通过
     * REJECTED = 已拒绝
     */
    @Column(name="audit_status", nullable = false, length = 20)
    private String auditStatus = "PENDING";

    /**
     * 拒绝原因
     */
    @Column(name="audit_reason", length = 255)
    private String auditReason;

    /**
     * 浏览量
     */
    @Column(name="view_count", nullable = false)
    private Integer viewCount = 0;

    /**
     * 点赞数
     */
    @Column(name="like_count", nullable = false)
    private Integer likeCount = 0;

    /**
     * 创建时间
     */
    @Column(name="created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name="updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
