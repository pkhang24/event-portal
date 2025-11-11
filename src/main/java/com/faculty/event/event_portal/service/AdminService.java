package com.faculty.event.event_portal.service;

import com.faculty.event.event_portal.dto.UserResponse;
import java.util.List;
import java.util.Map;

public interface AdminService {
    // 1. Quản lý User
    List<UserResponse> getAllUsers();
    UserResponse updateUserRole(Long userId, String newRoleName);

    // 2. Quản lý Sự kiện (Duyệt bài)
    void approveEvent(Long eventId);

    // 3. Thống kê tổng quan
    Map<String, Long> getDashboardStats();

    Map<String, Long> getEventRegistrationStats(); // Thống kê lượt đăng ký sự kiện
}