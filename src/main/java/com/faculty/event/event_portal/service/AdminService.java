package com.faculty.event.event_portal.service;

import com.faculty.event.event_portal.dto.*;
import com.faculty.event.event_portal.entity.Banner;
import com.faculty.event.event_portal.entity.Category;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface AdminService {
    // 1. Quản lý User
    List<UserResponse> getAllUsers();
    List<EventResponse> getAllEventsForAdmin();
    void changePassword(String userEmail, ChangePasswordRequest request);
    void toggleUserLock(Long userId);

    // --- User Recycle Bin ---
    List<UserResponse> getDeletedUsers();
    void restoreUser(Long userId);
    void permanentDeleteUser(Long userId);

    // --- Event Recycle Bin ---
    List<EventResponse> getDeletedEvents();
    void restoreEvent(Long eventId);
    void permanentDeleteEvent(Long eventId);

    // Danh mục
    List<Category> getDeletedCategories();
    Category restoreCategory(Long id);
    void hardDeleteCategory(Long id);

    // Banner
    List<Banner> getDeletedBanners();
    Banner restoreBanner(Long id);
    void hardDeleteBanner(Long id);

    List<DashboardActivity> getRecentActivities();

    UserResponse updateUserRole(Long userId, String newRoleName);
    UserResponse createUser(CreateUserRequest request);
    UserResponse getMyProfile(String userEmail);
    UserResponse updateMyProfile(String userEmail, UpdateProfileRequest request);
    UserResponse updateUser(Long userId, UpdateUserRequest request);

    // 2. Quản lý Sự kiện (Duyệt bài)
    void approveEvent(Long eventId);

    // 3. Thống kê tổng quan
    Map<String, Long> getDashboardStats();

    Map<String, Long> getEventRegistrationStats(); // Thống kê lượt đăng ký sự kiện

    Map<Integer, Long> getMonthlyEventStats(int year);

    Map<String, Long> getTopCategoryStats(int year, int month);

    // Lấy thống kê sự kiện (có lọc)
    Map<String, Long> getTopEventStats(int year, int month);

    // Xuất báo cáo Excel cho sự kiện
    byte[] exportDashboardReport(int year) throws IOException;

    List<CategoryResponse> getAllCategories();
    Category createCategory(Category category);
    Category updateCategory(Long id, Category categoryDetails);
    void deleteCategory(Long id);
}