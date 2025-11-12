package com.faculty.event.event_portal.service;

import com.faculty.event.event_portal.dto.AuthResponse;
import com.faculty.event.event_portal.dto.LoginRequest;
//import com.faculty.event.event_portal.dto.RegisterRequest;

public interface AuthService {
//    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}