package com.faculty.event.event_portal.service;

import com.faculty.event.event_portal.dto.NotificationResponse;
import com.faculty.event.event_portal.entity.User;
import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getMyNotifications(String email);
    void markAsRead(Long id);
    void markAllAsRead(String email);
    void createNotification(User user, String title, String message, String type);
}