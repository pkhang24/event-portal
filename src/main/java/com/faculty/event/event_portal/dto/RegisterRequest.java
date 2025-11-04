package com.faculty.event.event_portal.dto;

import lombok.Data;

@Data // Tự tạo getter/setter
public class RegisterRequest {
    // Chúng ta cần 3 thông tin này để đăng ký 1 sinh viên
    private String hoTen;
    private String mssv;
    private String email;
    private String password;
}