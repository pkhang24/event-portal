package com.faculty.event.event_portal.controller;

import com.faculty.event.event_portal.dto.*;
import com.faculty.event.event_portal.entity.Banner;
import com.faculty.event.event_portal.service.AdminService;
import com.faculty.event.event_portal.service.BannerService;
import com.faculty.event.event_portal.service.EventService;
import com.faculty.event.event_portal.repository.UserRepository;
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
    // Inject BannerService vào
    private final BannerService bannerService;
    // Inject UserRepository
    private final UserRepository userRepository;

    // Cập nhật constructor
    public AdminController(AdminService adminService,
                           EventService eventService,
                           BannerService bannerService,
                           UserRepository userRepository) { // Thêm
        this.adminService = adminService;
        this.eventService = eventService;
        this.bannerService = bannerService; // Thêm
        this.userRepository = userRepository;
    }

    // --- 1. QUẢN LÝ USER ---

    // GET /api/admin/users: Xem tất cả user
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // POST /api/admin/users
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        UserResponse newUser = adminService.createUser(request);
        return ResponseEntity.status(201).body(newUser);
    }

    // PUT /api/admin/users/{id}/role: Phân quyền cho user
    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long id,
                                                       @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(adminService.updateUserRole(id, request.getRole()));
    }

    // PUT /api/admin/users/{id}
    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(adminService.updateUser(id, request));
    }

    //
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Long id) {
        // Gọi thẳng vào repository vì @SQLDelete sẽ xử lý
        // (Hoặc bạn có thể tạo hàm trong Service)
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
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

    // --- 4. QUẢN LÝ BANNER (CRUD) ---

    // GET /api/admin/banners
    @GetMapping("/banners")
    public ResponseEntity<List<Banner>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    // POST /api/admin/banners
    @PostMapping("/banners")
    public ResponseEntity<Banner> createBanner(@RequestBody Banner banner) {
        return ResponseEntity.status(201).body(bannerService.createBanner(banner));
    }

    // PUT /api/admin/banners/{id}
    @PutMapping("/banners/{id}")
    public ResponseEntity<Banner> updateBanner(@PathVariable Long id, @RequestBody Banner bannerDetails) {
        return ResponseEntity.ok(bannerService.updateBanner(id, bannerDetails));
    }

    // DELETE /api/admin/banners/{id}
    @DeleteMapping("/banners/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }

    // API 5: (Admin) Lấy TẤT CẢ sự kiện (draft, published)
    // GET /api/admin/events
    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> getAllEventsForAdmin() {
        return ResponseEntity.ok(adminService.getAllEventsForAdmin());
    }

    // --- USER TRASH ---
// GET /api/admin/users/trash
    @GetMapping("/users/trash")
    public ResponseEntity<List<UserResponse>> getDeletedUsers() {
        return ResponseEntity.ok(adminService.getDeletedUsers());
    }

    // POST /api/admin/users/trash/{id}/restore
    @PostMapping("/users/trash/{id}/restore")
    public ResponseEntity<Void> restoreUser(@PathVariable Long id) {
        adminService.restoreUser(id);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/admin/users/trash/{id}/permanent
    @DeleteMapping("/users/trash/{id}/permanent")
    public ResponseEntity<Void> permanentDeleteUser(@PathVariable Long id) {
        adminService.permanentDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // --- EVENT TRASH ---
// GET /api/admin/events/trash
    @GetMapping("/events/trash")
    public ResponseEntity<List<EventResponse>> getDeletedEvents() {
        return ResponseEntity.ok(adminService.getDeletedEvents());
    }

    // POST /api/admin/events/trash/{id}/restore
    @PostMapping("/events/trash/{id}/restore")
    public ResponseEntity<Void> restoreEvent(@PathVariable Long id) {
        adminService.restoreEvent(id);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/admin/events/trash/{id}/permanent
    @DeleteMapping("/events/trash/{id}/permanent")
    public ResponseEntity<Void> permanentDeleteEvent(@PathVariable Long id) {
        adminService.permanentDeleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}