package com.faculty.event.event_portal.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    // Chỉ cho phép cập nhật 2 trường này
    private String email;
    private String soDienThoai;
}