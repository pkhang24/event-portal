package com.faculty.event.event_portal.controller;

import com.faculty.event.event_portal.dto.*;
import com.faculty.event.event_portal.entity.Banner;
import com.faculty.event.event_portal.entity.Category;
import com.faculty.event.event_portal.service.AdminService;
import com.faculty.event.event_portal.service.BannerService;
import com.faculty.event.event_portal.service.EventService;
import com.faculty.event.event_portal.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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

    // GET /api/admin/dashboard/activities
    @GetMapping("/dashboard/activities")
    public ResponseEntity<List<DashboardActivity>> getRecentActivities() {
        return ResponseEntity.ok(adminService.getRecentActivities());
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

    // DEL /api/admin/users/{id}
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Long id) {
        // Gọi thẳng vào repository vì @SQLDelete sẽ xử lý
        // (Hoặc bạn có thể tạo hàm trong Service)
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // PUT /api/admin/users/{id}/lock
    @PutMapping("/users/{id}/lock")
    public ResponseEntity<Void> toggleUserLock(@PathVariable Long id) {
        adminService.toggleUserLock(id);
        return ResponseEntity.ok().build();
    }

    // --- 2. QUẢN LÝ SỰ KIỆN ---

    // PUT /api/admin/events/{id}/approve: Duyệt bài (DRAFT -> PUBLISHED)
    @PutMapping("/events/{id}/approve")
    public ResponseEntity<Void> approveEvent(@PathVariable Long id) {
        adminService.approveEvent(id);
        return ResponseEntity.ok().build();
    }

    // --- QUẢN LÝ CATEGORY ---
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() { // Sửa kiểu trả về
        return ResponseEntity.ok(adminService.getAllCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        return ResponseEntity.status(201).body(adminService.createCategory(category));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category categoryDetails) {
        return ResponseEntity.ok(adminService.updateCategory(id, categoryDetails));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return ResponseEntity.noContent().build();
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

    // === Category Trash ===
    @GetMapping("/categories/trash")
    public ResponseEntity<List<Category>> getDeletedCategories() {
        return ResponseEntity.ok(adminService.getDeletedCategories());
    }

    @PutMapping("/categories/trash/restore/{id}")
    public ResponseEntity<Category> restoreCategory(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.restoreCategory(id));
    }

    @DeleteMapping("/categories/trash/hard-delete/{id}")
    public ResponseEntity<Void> hardDeleteCategory(@PathVariable Long id) {
        adminService.hardDeleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // === Banner Trash ===
    @GetMapping("/banners/trash")
    public ResponseEntity<List<Banner>> getDeletedBanners() {
        return ResponseEntity.ok(adminService.getDeletedBanners());
    }

    @PutMapping("/banners/trash/restore/{id}")
    public ResponseEntity<Banner> restoreBanner(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.restoreBanner(id));
    }

    @DeleteMapping("/banners/trash/hard-delete/{id}")
    public ResponseEntity<Void> hardDeleteBanner(@PathVariable Long id) {
        adminService.hardDeleteBanner(id);
        return ResponseEntity.noContent().build();
    }

    // API 6: Lấy thống kê Top sự kiện (theo năm, tháng)
    // GET /api/admin/stats/top-events?year=2025&month=11
    @GetMapping("/stats/top-events")
    public ResponseEntity<Map<String, Long>> getTopEventStats(
            @RequestParam int year,
            @RequestParam(required = false, defaultValue = "0") int month) {
        return ResponseEntity.ok(adminService.getTopEventStats(year, month));
    }

    // GET /api/admin/stats/monthly-events?year=2025
    @GetMapping("/stats/monthly-events")
    public ResponseEntity<Map<Integer, Long>> getMonthlyEventStats(@RequestParam int year) {
        return ResponseEntity.ok(adminService.getMonthlyEventStats(year));
    }

    // GET /api/admin/stats/top-categories
    @GetMapping("/stats/top-categories")
    public ResponseEntity<Map<String, Long>> getTopCategoryStats() {
        return ResponseEntity.ok(adminService.getTopCategoryStats());
    }

    // API 7: Xuất báo cáo Excel
    // GET /api/admin/report/events-excel
    @GetMapping("/report/events-excel")
    public ResponseEntity<byte[]> exportEventsReport() {
        try {
            byte[] excelData = adminService.exportEventsToExcel();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "bao-cao-su-kien.xlsx");

            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}