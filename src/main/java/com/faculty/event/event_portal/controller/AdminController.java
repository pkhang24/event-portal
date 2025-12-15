package com.faculty.event.event_portal.controller;

import com.faculty.event.event_portal.dto.*;
import com.faculty.event.event_portal.entity.Banner;
import com.faculty.event.event_portal.entity.Category;
import com.faculty.event.event_portal.entity.User;
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
public class AdminController {

    private final AdminService adminService;
    private final EventService eventService;
    private final BannerService bannerService;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService,
                           EventService eventService,
                           BannerService bannerService,
                           UserRepository userRepository) {
        this.adminService = adminService;
        this.eventService = eventService;
        this.bannerService = bannerService;
        this.userRepository = userRepository;
    }

    // --- DASHBOARD ---
    @GetMapping("/dashboard/activities")
    public ResponseEntity<List<DashboardActivity>> getRecentActivities() {
        return ResponseEntity.ok(adminService.getRecentActivities());
    }

    // --- 1. QUẢN LÝ USER ---
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        UserResponse newUser = adminService.createUser(request);
        return ResponseEntity.status(201).body(newUser);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(adminService.updateUserRole(id, request.getRole()));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(adminService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Long id) {
//        userRepository.deleteById(id);
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/lock")
    public ResponseEntity<Void> toggleUserLock(@PathVariable Long id) {
        adminService.toggleUserLock(id);
        return ResponseEntity.ok().build();
    }

    // --- 2. QUẢN LÝ SỰ KIỆN (ADMIN) ---

    // 👇 [SỬA LẠI CHỖ NÀY]: Bạn phải mở comment ra thì Admin mới thấy danh sách
    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEventsForAdmin());
    }

    @PutMapping("/events/{id}/approve")
    public ResponseEntity<Void> approveEvent(@PathVariable Long id) {
        adminService.approveEvent(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/events/{id}/reject")
    public ResponseEntity<Void> rejectEvent(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "";
        eventService.rejectEvent(id, reason);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/events/{id}/cancel")
    public ResponseEntity<Void> cancelEvent(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "";
        eventService.cancelEvent(id, reason);
        return ResponseEntity.ok().build();
    }

    // --- QUẢN LÝ CATEGORY ---
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
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

    // --- 3. THỐNG KÊ ---
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/event-stats")
    public ResponseEntity<Map<String, Long>> getEventRegistrationStats() {
        return ResponseEntity.ok(adminService.getEventRegistrationStats());
    }

    // --- USER TRASH ---
    @GetMapping("/users/trash")
    public ResponseEntity<List<UserResponse>> getDeletedUsers() {
        return ResponseEntity.ok(adminService.getDeletedUsers());
    }

    @PostMapping("/users/trash/{id}/restore")
    public ResponseEntity<Void> restoreUser(@PathVariable Long id) {
        adminService.restoreUser(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/trash/{id}/permanent")
    public ResponseEntity<Void> permanentDeleteUser(@PathVariable Long id) {
        adminService.permanentDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // --- EVENT TRASH ---
    @GetMapping("/events/trash")
    public ResponseEntity<List<EventResponse>> getDeletedEvents() {
        return ResponseEntity.ok(adminService.getDeletedEvents());
    }

    @PostMapping("/events/trash/{id}/restore")
    public ResponseEntity<Void> restoreEvent(@PathVariable Long id) {
        adminService.restoreEvent(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/events/trash/{id}/permanent")
    public ResponseEntity<Void> permanentDeleteEvent(@PathVariable Long id) {
        adminService.permanentDeleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    // --- CATEGORY TRASH ---
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

    // --- STATS & REPORTS ---
    @GetMapping("/stats/top-events")
    public ResponseEntity<Map<String, Long>> getTopEventStats(
            @RequestParam int year,
            @RequestParam(required = false, defaultValue = "0") int month) {
        return ResponseEntity.ok(adminService.getTopEventStats(year, month));
    }

    @GetMapping("/stats/monthly-events")
    public ResponseEntity<Map<Integer, Long>> getMonthlyEventStats(@RequestParam int year) {
        return ResponseEntity.ok(adminService.getMonthlyEventStats(year));
    }

    @GetMapping("/stats/top-categories")
    public ResponseEntity<Map<String, Long>> getTopCategoryStats(
            @RequestParam int year,
            @RequestParam(required = false, defaultValue = "0") int month) {

        return ResponseEntity.ok(adminService.getTopCategoryStats(year, month));
    }

    @GetMapping("/report/events-excel")
    public ResponseEntity<byte[]> exportDashboardReport(@RequestParam(defaultValue = "2025") int year) {
        try {
            byte[] excelData = adminService.exportDashboardReport(year);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Bao_cao_thong_ke_" + year + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}