package com.faculty.event.event_portal.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "categories")
// 1. Chỉ định UPDATE thay vì DELETE khi gọi hàm xóa
@SQLDelete(sql = "UPDATE categories SET deleted_at = NOW() WHERE id = ?")
// 2. Thêm điều kiện mặc định: Chỉ lấy các bản ghi chưa bị xóa
@Where(clause = "deleted_at IS NULL")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tenDanhMuc;

    // 3. Thêm trường đánh dấu thời gian xóa
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // Trường này sẽ chứa thời gian xóa

    // (Bạn có thể thêm mô tả nếu muốn)
}