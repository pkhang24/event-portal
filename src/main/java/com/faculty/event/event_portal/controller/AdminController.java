package com.faculty.event.event_portal.controller;

import com.faculty.event.event_portal.dto.EventResponse;
import com.faculty.event.event_portal.dto.UpdateRoleRequest;
import com.faculty.event.event_portal.dto.UserResponse;
import com.faculty.event.event_portal.service.AdminService;
import com.faculty.event.event_portal.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
// Đã được bảo vệ bởi SecurityConfig: .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
public class AdminController {

    private final AdminService adminService;
    // Admin cũng cần dùng một số hàm của EventService (như xem chi tiết sự kiện)
    private final EventService eventService;

    public AdminController(AdminService adminService, EventService eventService) {
        this.adminService = adminService;
        this.eventService = eventService;
    }

    // --- 1. QUẢN LÝ USER ---

    // GET /api/admin/users: Xem tất cả user
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // PUT /api/admin/users/{id}/role: Phân quyền cho user
    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long id,
                                                       @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(adminService.updateUserRole(id, request.getRole()));
    }

    // --- 2. QUẢN LÝ SỰ KIỆN ---

    // PUT /api/admin/events/{id}/approve: Duyệt bài (DRAFT -> PUBLISHED)
    @PutMapping("/events/{id}/approve")
    public ResponseEntity<Void> approveEvent(@PathVariable Long id) {
        adminService.approveEvent(id);
        return ResponseEntity.ok().build();
    }

    // (Admin có thể dùng DELETE /api/events/{id} của EventController để xóa sự kiện)

    // --- 3. THỐNG KÊ ---
    // GET /api/admin/stats: Lấy số liệu tổng quan cho Dashboard
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // GET /api/admin/event-stats
    @GetMapping("/event-stats")
    public ResponseEntity<Map<String, Long>> getEventRegistrationStats() {
        return ResponseEntity.ok(adminService.getEventRegistrationStats());
    }
}