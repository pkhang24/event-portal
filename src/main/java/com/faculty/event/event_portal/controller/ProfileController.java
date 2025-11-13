package com.faculty.event.event_portal.controller;

import com.faculty.event.event_portal.dto.ChangePasswordRequest;
import com.faculty.event.event_portal.dto.UpdateProfileRequest;
import com.faculty.event.event_portal.dto.UserResponse;
import com.faculty.event.event_portal.service.AdminService; // Dùng chung AdminService
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    // Chúng ta dùng tạm AdminService vì nó có hàm convert
    private final AdminService adminService;

    public ProfileController(AdminService adminService) {
        this.adminService = adminService;
    }

    // GET /api/profile/me
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(adminService.getMyProfile(email));
    }

    // PUT /api/profile/me
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(Authentication authentication,
                                                        @RequestBody UpdateProfileRequest request) {
        String email = authentication.getName();
        return ResponseEntity.ok(adminService.updateMyProfile(email, request));
    }

    // POST /api/profile/change-password
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @RequestBody ChangePasswordRequest request) {
        adminService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok().build();
    }
}