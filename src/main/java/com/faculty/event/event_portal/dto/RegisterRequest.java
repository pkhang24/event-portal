package com.faculty.event.event_portal.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String hoTen;
    private String mssv;
    private String email;
    private String password;
}