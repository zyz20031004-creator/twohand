package com.campus.twohand.chat.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_session")
@Data
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_low", nullable = false)
    private Long userLow;

    @Column(name = "user_high", nullable = false)
    private Long userHigh;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "last_msg", length = 255)
    private String lastMsg;

    @Column(name = "last_time")
    private LocalDateTime lastTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (lastTime == null) {
            lastTime = now;
        }
    }
}
