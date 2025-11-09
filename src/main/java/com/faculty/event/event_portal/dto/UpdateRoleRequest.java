package com.faculty.event.event_portal.dto;

import lombok.Data;

@Data
public class UpdateRoleRequest {
    private String role; // Admin sẽ gửi lên "POSTER" hoặc "ADMIN"
}