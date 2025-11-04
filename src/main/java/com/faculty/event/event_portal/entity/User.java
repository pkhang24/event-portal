package com.faculty.event.event_portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data // Của Lombok: Tự động tạo Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // Của Lombok: Tự động tạo constructor rỗng
@AllArgsConstructor // Của Lombok: Tự động tạo constructor có đủ tham số
@Entity // Đánh dấu đây là một Entity
@Table(name = "users") // Tên bảng trong CSDL
public class User {

    @Id // Đánh dấu đây là khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Khóa chính tự tăng
    private Long id;

    @Column(nullable = false) // Cột không được null
    private String hoTen;

    @Column(nullable = false, unique = true) // Cột không được null và phải là duy nhất
    private String email;

    @Column(nullable = false)
    private String password; // Mật khẩu này sẽ được mã hóa

    @Column(nullable = true) // Cột này có thể null (vì Admin/Poster không có)
    private String mssv;

    @Enumerated(EnumType.STRING) // Báo cho JPA biết lưu Enum này dưới dạng CHUỖI
    @Column(nullable = false)
    private Role role; // Sử dụng Enum ta vừa tạo

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // Thời gian tạo

    // Tự động gán thời gian hiện tại trước khi lưu
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}