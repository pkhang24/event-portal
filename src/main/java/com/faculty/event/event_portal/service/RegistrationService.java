package com.faculty.event.event_portal.service;

import com.faculty.event.event_portal.dto.RegistrationRequest;
import com.faculty.event.event_portal.dto.TicketResponse;

import java.util.List;

public interface RegistrationService {

    // Sinh viên đăng ký tham gia
    TicketResponse createRegistration(RegistrationRequest request, String studentEmail);
    void cancelRegistration(Long registrationId, String studentEmail);

    // Sinh viên xem "Vé của tôi"
    List<TicketResponse> getMyTickets(String studentEmail);

    // Poster/Admin điểm danh (check-in)
    TicketResponse checkInTicket(String ticketCode);

    List<TicketResponse> getMyHistory(String studentEmail);
}