package com.faculty.event.event_portal.dto;

import lombok.Data;

@Data
public class CategoryResponse {
    private Long id;
    private String tenDanhMuc;
    private Long soLuongSuKien; // Trường mới
}