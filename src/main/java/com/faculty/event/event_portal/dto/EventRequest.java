package com.faculty.event.event_portal.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventRequest {
    private String tieuDe;
    private String moTaNgan;
    private String noiDung;
    private String anhThumbnail; // URL ảnh
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private String diaDiem;
    private Integer soLuongGioiHan; // Có thể null
    private Long categoryId;
    private String trangThai;
    // "trangThai" (DRAFT/PUBLISHED)
}