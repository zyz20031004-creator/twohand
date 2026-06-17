package com.campus.twohand.order.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "buyer_id")
    private Long buyerId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_title")
    private String productTitle;

    @Column(name = "product_price")
    private BigDecimal productPrice;

    private BigDecimal amount;

    /** UNPAID / PAID / CANCELLED / FINISHED */
    private String status;

    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "trade_location")
    private String tradeLocation;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "address_text")
    private String addressText;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "pay_type")
    private String payType;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "buyer_rate")
    private String buyerRate;

    @Column(name = "buyer_comment")
    private String buyerComment;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "buyer_visible", nullable = false)
    private Boolean buyerVisible = Boolean.TRUE;

    @Column(name = "seller_visible", nullable = false)
    private Boolean sellerVisible = Boolean.TRUE;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
