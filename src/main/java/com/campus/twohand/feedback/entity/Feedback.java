package com.campus.twohand.feedback.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 可空：前台未登录也可提交（你想强制登录再改）
    @Column(name = "user_id")
    private Long userId;

    private String subject;

    @Column(columnDefinition = "text")
    private String content;

    private String contact;

    private String email;

    // OPEN / CLOSED
    private String status;

    @Column(columnDefinition = "text")
    private String reply;

    @Column(name = "replied_by")
    private Long repliedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;
}
