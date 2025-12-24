package com.faculty.event.event_portal.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventResponse {
    private Long id;
    private String tieuDe;
    private String moTaNgan;
    private String noiDung;
    private String anhThumbnail;
    private String anhBia;
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private String diaDiem;
    private Integer soLuongGioiHan;
    private Long soNguoiDaDangKy;
    private String trangThai;
    private int luotXem;
    private String tenNguoiDang;
    private String tenDanhMuc;
    private Long categoryId;
    private LocalDateTime createdAt;
    private Boolean isRegistered;
//    private boolean deleted;
}