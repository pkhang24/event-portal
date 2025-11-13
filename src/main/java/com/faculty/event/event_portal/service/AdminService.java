package com.faculty.event.event_portal.service;

import com.faculty.event.event_portal.dto.*;

import java.util.List;
import java.util.Map;

public interface AdminService {
    // 1. Quản lý User
    List<UserResponse> getAllUsers();
    List<EventResponse> getAllEventsForAdmin();
    void changePassword(String userEmail, ChangePasswordRequest request);
    // --- User Recycle Bin ---
    List<UserResponse> getDeletedUsers();
    void restoreUser(Long userId);
    void permanentDeleteUser(Long userId);

    // --- Event Recycle Bin ---
    List<EventResponse> getDeletedEvents();
    void restoreEvent(Long eventId);
    void permanentDeleteEvent(Long eventId);

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
}