package com.faculty.event.event_portal.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventResponse {
    // Trả về các thông tin này cho frontend
    private Long id;
    private String tieuDe;
    private String moTaNgan;
    private String noiDung; // Cần cho trang chi tiết
    private String anhThumbnail;
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private String diaDiem;
    private Integer soLuongGioiHan;
    private Long soNguoiDaDangKy;
    private String trangThai; // "DRAFT" hoặc "PUBLISHED"
    private int luotXem;
    private String tenNguoiDang; // Thêm thông tin người đăng cho đẹp
    private String tenDanhMuc;
    private Long categoryId;
    private LocalDateTime createdAt;
    private Boolean isRegistered; // Thêm trường này (có thể null nếu user chưa đăng nhập)
//    private boolean deleted;
}