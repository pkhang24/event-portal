package com.faculty.event.event_portal.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String hoTen;
    private String mssv;
    private String soDienThoai;
    private String nganhHoc;
    private String lopHoc;
    private String khoa;
}