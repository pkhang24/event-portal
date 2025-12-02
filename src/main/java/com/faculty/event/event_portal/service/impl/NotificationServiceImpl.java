package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.dto.NotificationResponse;
import com.faculty.event.event_portal.entity.Notification;
import com.faculty.event.event_portal.entity.User;
import com.faculty.event.event_portal.repository.NotificationRepository;
import com.faculty.event.event_portal.repository.UserRepository;
import com.faculty.event.event_portal.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<NotificationResponse> getMyNotifications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification != null) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        List<Notification> list = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        for (Notification n : list) {
            if (!n.isRead()) {
                n.setRead(true);
            }
        }
        notificationRepository.saveAll(list);
    }

    @Override
    public void createNotification(User user, String title, String message, String type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notificationRepository.save(notification);
    }

    private NotificationResponse convertToResponse(Notification n) {
        NotificationResponse dto = new NotificationResponse();
        dto.setId(n.getId());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setType(n.getType());
        dto.setRead(n.isRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}