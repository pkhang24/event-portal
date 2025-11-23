package com.faculty.event.event_portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import java.time.LocalDateTime;

@Data // Của Lombok: Tự động tạo Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // Của Lombok: Tự động tạo constructor rỗng
@AllArgsConstructor // Của Lombok: Tự động tạo constructor có đủ tham số
@Entity // Đánh dấu đây là một Entity
@Table(name = "users") // Tên bảng trong CSDL
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE id = ?") // Tự động chạy khi gọi delete()
@Where(clause = "deleted_at IS NULL") // Tự động thêm vào MỌI câu lệnh SELECT

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

    @Column(nullable = true, unique = true) // Cột này có thể null (vì Admin/Poster không có)
    private String mssv;

    @Column(nullable = true)
    private String soDienThoai;

    @Column(nullable = true)
    private String nganhHoc;

    @Column(nullable = true)
    private String lopHoc;

    @Column(nullable = true)
    private String khoa;

    @Enumerated(EnumType.STRING) // Báo cho JPA biết lưu Enum này dưới dạng CHUỖI
    @Column(nullable = false)
    private Role role; // Sử dụng Enum ta vừa tạo

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // Thời gian tạo

    // --- THÊM TRƯỜNG MỚI NÀY ---
    @Column(nullable = true)
    private LocalDateTime deletedAt;

    // Tự động gán thời gian hiện tại trước khi lưu
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}