package com.faculty.event.event_portal.entity;

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
@Table(name = "events")
@SQLDelete(sql = "UPDATE events SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")

public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tieuDe;

    @Column(columnDefinition = "TEXT") // Dùng TEXT cho mô tả ngắn
    private String moTaNgan;

    @Column(columnDefinition = "TEXT") // Dùng TEXT cho nội dung dài
    private String noiDung;

    private String anhThumbnail; // URL của ảnh

    @Column(nullable = false)
    private LocalDateTime thoiGianBatDau;

    @Column(nullable = false)
    private LocalDateTime thoiGianKetThuc;

    private String diaDiem;

    @Column(nullable = true) // Có thể null = không giới hạn
    private Integer soLuongGioiHan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus trangThai;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int luotXem = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- THÊM TRƯỜNG MỚI NÀY ---
    @Column(nullable = true)
    private LocalDateTime deletedAt;

    // --- Định nghĩa Quan hệ (Relationship) ---

    // (FK) Khóa ngoại: nguoi_dang_id
    // Nhiều sự kiện (Many) thuộc về một người đăng (One)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dang_id", nullable = false)
    private User nguoiDang; // JPA sẽ tự động hiểu đây là khóa ngoại tới bảng User

    // Tự động gán thời gian hiện tại
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = true) // Cho phép null nếu chưa phân loại
    private Category category;
}