package com.faculty.event.event_portal.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    // Chỉ cho phép sửa các trường này
    private String hoTen;
    private String mssv;
    private String soDienThoai;
    private String nganhHoc;
    private String lopHoc;
    private String khoa;
    // Email và Role được quản lý riêng
}