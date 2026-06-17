package com.campus.twohand.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminOrderPageRow {
    private Long id;
    private String orderNo;

    private Long productId;
    private String productTitle;
    private String coverUrl;

    private BigDecimal amount;
    private String status;

    private Long buyerId;
    private String buyerName;

    private Long sellerId;
    private String sellerName;

    private Long addressId;
    private String receiver;
    private String phone;
    private String receiveAddress;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime finishedAt;
}