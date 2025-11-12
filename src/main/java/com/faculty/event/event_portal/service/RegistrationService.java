package com.faculty.event.event_portal.service;

import com.faculty.event.event_portal.dto.RegistrationRequest;
import com.faculty.event.event_portal.dto.TicketResponse;

import java.util.List;

public interface RegistrationService {

    // 1. Sinh viên đăng ký tham gia
    TicketResponse createRegistration(RegistrationRequest request, String studentEmail);

    // 2. Sinh viên xem "Vé của tôi"
    List<TicketResponse> getMyTickets(String studentEmail);

    // 3. (Sẽ làm sau) Poster/Admin điểm danh (check-in)
    TicketResponse checkInTicket(String ticketCode);

    List<TicketResponse> getMyHistory(String studentEmail);
}