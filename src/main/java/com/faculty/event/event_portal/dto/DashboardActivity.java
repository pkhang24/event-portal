package com.faculty.event.event_portal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DashboardActivity {
    private String title;
    private LocalDateTime time; // Thời gian
    private String type;        // Loại (register, create_event, new_user...)
}