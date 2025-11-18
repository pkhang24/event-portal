package com.faculty.event.event_portal.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "banners")
// 1. Chỉ định UPDATE thay vì DELETE khi gọi hàm xóa
@SQLDelete(sql = "UPDATE banners SET deleted_at = NOW() WHERE id = ?")
// 2. Thêm điều kiện mặc định: Chỉ lấy các bản ghi chưa bị xóa
@Where(clause = "deleted_at IS NULL")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl; // URL của ảnh

    @Column(nullable = true) // Link có thể có hoặc không
    private String linkUrl;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @JsonProperty("isActive")
    private boolean isActive = true; // Mặc định là banner đang hoạt động

    // 3. Thêm trường đánh dấu thời gian xóa
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // Trường này sẽ chứa thời gian xóa

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}