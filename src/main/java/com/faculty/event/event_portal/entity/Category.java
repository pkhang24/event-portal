package com.faculty.event.event_portal.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tenDanhMuc;

    // (Bạn có thể thêm mô tả nếu muốn)
}