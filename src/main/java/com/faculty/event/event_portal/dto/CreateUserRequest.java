package com.faculty.event.event_portal.dto;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String hoTen;
    private String mssv;
    private String email;
    private String password; // Mật khẩu ban đầu
    private String soDienThoai;
    private String nganhHoc;
    private String lopHoc;
    private String khoa;
    private String role; // "STUDENT" hoặc "POSTER"
}