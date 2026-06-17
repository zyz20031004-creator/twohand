package com.campus.twohand.product.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏实体类：对应数据库 favorite 表
 * 用 status 做“软删除”：
 * 1=收藏有效，0=取消收藏
 */
@Data
@Entity
@Table(name = "favorite")
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "product_id")
    private Long productId;

    // 1有效 0取消
    private Integer status;

    /**
     * created_at 由数据库自动维护
     * insertable=false 表示 JPA 插入时不管它
     * updatable=false 表示 JPA 更新时也不管它
     */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * updated_at 由数据库自动维护
     */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
