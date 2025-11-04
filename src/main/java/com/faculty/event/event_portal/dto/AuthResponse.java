package com.faculty.event.event_portal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // Tạo constructor với 1 tham số
public class AuthResponse {
    // Chúng ta chỉ cần trả về token
    private String token;
}