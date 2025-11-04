package com.faculty.event.event_portal.controller;

import com.faculty.event.event_portal.dto.CheckInRequest;
import com.faculty.event.event_portal.dto.RegistrationRequest;
import com.faculty.event.event_portal.dto.TicketResponse;
import com.faculty.event.event_portal.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    // API 1: Sinh viên đăng ký tham gia sự kiện
    // YÊU CẦU VAI TRÒ STUDENT
    // POST http://localhost:8080/api/registrations
    @PostMapping
    public ResponseEntity<TicketResponse> createRegistration(@RequestBody RegistrationRequest request,
                                                             Authentication authentication) {
        String studentEmail = authentication.getName();
        TicketResponse ticket = registrationService.createRegistration(request, studentEmail);
        return ResponseEntity.status(201).body(ticket); // 201 Created
    }

    // API 2: Lấy tất cả vé của tôi (của sinh viên đang đăng nhập)
    // YÊU CẦU VAI TRÒ STUDENT
    // GET http://localhost:8080/api/registrations/my-tickets
    @GetMapping("/my-tickets")
    public ResponseEntity<List<TicketResponse>> getMyTickets(Authentication authentication) {
        String studentEmail = authentication.getName();
        List<TicketResponse> tickets = registrationService.getMyTickets(studentEmail);
        return ResponseEntity.ok(tickets);
    }

    // API 3: Điểm danh (Check-in)
    // YÊU CẦU VAI TRÒ POSTER hoặc ADMIN
// POST http://localhost:8080/api/registrations/check-in
    @PostMapping("/check-in")
    public ResponseEntity<TicketResponse> checkIn(@RequestBody CheckInRequest request,
                                                  Authentication authentication) {
        // Chúng ta có thể log lại email người check-in (authentication.getName()) nếu muốn
        TicketResponse ticket = registrationService.checkInTicket(request.getTicketCode());
        return ResponseEntity.ok(ticket);
    }
}