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

    // POST http://localhost:8080/api/registrations
    @PostMapping
    public ResponseEntity<TicketResponse> createRegistration(@RequestBody RegistrationRequest request,
                                                             Authentication authentication) {
        String studentEmail = authentication.getName();
        TicketResponse ticket = registrationService.createRegistration(request, studentEmail);
        return ResponseEntity.status(201).body(ticket); // 201 Created
    }

    // GET http://localhost:8080/api/registrations/my-tickets
    @GetMapping("/my-tickets")
    public ResponseEntity<List<TicketResponse>> getMyTickets(Authentication authentication) {
        String studentEmail = authentication.getName();
        List<TicketResponse> tickets = registrationService.getMyTickets(studentEmail);
        return ResponseEntity.ok(tickets);
    }

    // POST http://localhost:8080/api/registrations/check-in
    @PostMapping("/check-in")
    public ResponseEntity<TicketResponse> checkIn(@RequestBody CheckInRequest request,
                                                  Authentication authentication) {
        TicketResponse ticket = registrationService.checkInTicket(request.getTicketCode());
        return ResponseEntity.ok(ticket);
    }

    // GET http://localhost:8080/api/registrations/history
    @GetMapping("/history")
    public ResponseEntity<List<TicketResponse>> getMyHistory(Authentication authentication) {
        String studentEmail = authentication.getName();
        List<TicketResponse> history = registrationService.getMyHistory(studentEmail);
        return ResponseEntity.ok(history);
    }

    // DELETE http://localhost:8080/api/registrations/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelRegistration(@PathVariable("id") Long registrationId,
                                                   Authentication authentication) {
        String studentEmail = authentication.getName();
        registrationService.cancelRegistration(registrationId, studentEmail);
        return ResponseEntity.noContent().build();
    }
}