package com.faculty.event.event_portal.controller;

import com.faculty.event.event_portal.dto.AuthResponse;
import com.faculty.event.event_portal.dto.LoginRequest;
//import com.faculty.event.event_portal.dto.RegisterRequest;
import com.faculty.event.event_portal.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth") // Tiền tố chung cho các API xác thực
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // API Đăng ký
    // POST http://localhost:8080/api/auth/register
//    @PostMapping("/register")
//    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
//        // Gọi service để xử lý
//        AuthResponse response = authService.register(request);
//        return ResponseEntity.ok(response);
//    }

    // API Đăng nhập
    // POST http://localhost:8080/api/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // Gọi service để xử lý
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}