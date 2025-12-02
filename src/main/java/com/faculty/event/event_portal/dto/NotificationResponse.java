package com.faculty.event.event_portal.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private LocalDateTime createdAt;
}