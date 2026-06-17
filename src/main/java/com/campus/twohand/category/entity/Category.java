package com.campus.twohand.category.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 分类名称
    @Column(nullable = false, length = 50, unique = true)
    private String name;

    // 排序
    @Column(nullable = false)
    private Integer sort = 0;

    // 状态：1启用 0禁用（表里有这个字段）
    @Column(nullable = false)
    private Integer status = 1;

    // created_at 由数据库自动生成
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // updated_at 由数据库自动更新
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}