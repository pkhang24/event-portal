package com.faculty.event.event_portal.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventRequest {
    // Chúng ta cần các trường này từ người đăng (POSTER)
    private String tieuDe;
    private String moTaNgan;
    private String noiDung;
    private String anhThumbnail; // URL ảnh
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private String diaDiem;
    private Integer soLuongGioiHan; // Có thể null (không giới hạn)
    // "trangThai" (DRAFT/PUBLISHED) sẽ được set trong service
}