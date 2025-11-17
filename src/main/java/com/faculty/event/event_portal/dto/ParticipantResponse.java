package com.faculty.event.event_portal.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ParticipantResponse {
    private Long userId;
    private String hoTen;
    private String mssv;
    private String email;
    private String lopHoc;
    private String trangThaiVe; // (Đã đăng ký / Đã tham gia)
    private LocalDateTime thoiGianDangKy;
}