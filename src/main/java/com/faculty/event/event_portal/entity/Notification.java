package com.faculty.event.event_portal.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String message;

    // Loại thông báo (ví dụ: INFO, WARNING, SUCCESS) để hiển thị icon khác nhau
    private String type = "INFO";

    @Column(name = "is_read")
    private boolean isRead = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Thông báo thuộc về user nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}