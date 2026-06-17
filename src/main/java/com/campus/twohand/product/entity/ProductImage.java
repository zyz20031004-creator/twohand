package com.campus.twohand.product.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 商品图片实体类
 * 对应数据库表：product_image
 */
@Data
@Entity
@Table(name = "product_image")
public class ProductImage {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 对应的商品ID（product 表的 id）
     * 注意：这里我们先不加外键，方便开发（小白更容易跑通）
     */
    @Column(name = "product_id")
    private Long productId;

    /**
     * 图片URL（你数据库里存的是 /upload/xxx.jpg 这种路径）
     */
    private String url;

    /**
     * 排序字段（越小越靠前）
     * 我们会取 sort 最小的一张作为封面图 coverUrl
     */
    private Integer sort;
}
