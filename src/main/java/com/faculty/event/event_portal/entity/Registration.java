package com.faculty.event.event_portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID; // Dùng để tạo mã vé

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "registrations",
        // Thêm một ràng buộc: không cho phép 1 user đăng ký 1 event 2 lần
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "event_id"})
        }
)
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // (FK) Khóa ngoại: user_id
    // Nhiều lượt đăng ký (Many) thuộc về một User (One)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // (FK) Khóa ngoại: event_id
    // Nhiều lượt đăng ký (Many) thuộc về một Event (One)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, unique = true)
    private String ticketCode; // Mã vé (dùng cho QR code)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus trangThai;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Tự động gán giá trị trước khi lưu
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // Tự động tạo một mã vé duy nhất
        if (ticketCode == null) {
            ticketCode = UUID.randomUUID().toString();
        }
        // Gán trạng thái mặc định
        if (trangThai == null) {
            trangThai = RegistrationStatus.REGISTERED;
        }
    }
}