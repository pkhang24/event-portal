package com.faculty.event.event_portal.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TicketResponse {
    // Thông tin vé
    private Long registrationId;
    private String ticketCode; // Mã QR code
    private String trangThai; // "REGISTERED" or "ATTENDED"

    // Thông tin sự kiện
    private Long eventId;
    private String tieuDeSuKien;
    private LocalDateTime thoiGianBatDau;
    private String diaDiem;

    // Thông tin người tham dự
    private Long studentId;
    private String tenSinhVien;
}