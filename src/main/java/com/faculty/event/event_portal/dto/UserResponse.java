package com.faculty.event.event_portal.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String hoTen;
    private String email;
    private String mssv;
    private String role;
    private String soDienThoai;
    private String khoa;
    private String lopHoc;
    private String nganhHoc;
    private LocalDateTime createdAt;
}