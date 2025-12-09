package com.faculty.event.event_portal.controller;

import com.faculty.event.event_portal.dto.EventRequest;
import com.faculty.event.event_portal.dto.EventResponse;
import com.faculty.event.event_portal.dto.ParticipantResponse;
import com.faculty.event.event_portal.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // API 1: Lấy danh sách sự kiện (cho trang chủ) - PUBLIC
    // GET http://localhost:8080/api/events
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllPublishedEvents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId
    ) {
        List<EventResponse> events = eventService.getAllPublishedEvents(search, status, categoryId);
        return ResponseEntity.ok(events);
    }

    // API 2: Lấy chi tiết 1 sự kiện - PUBLIC
    // GET http://localhost:8080/api/events/1
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        EventResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    // API 3: Tạo sự kiện mới - YÊU CẦU VAI TRÒ POSTER
    // POST http://localhost:8080/api/events
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@RequestBody EventRequest request,
                                                     Authentication authentication) {
        // Lấy email của POSTER từ token đã xác thực
        String email = authentication.getName();
        EventResponse createdEvent = eventService.createEvent(request, email);
        return ResponseEntity.status(201).body(createdEvent); // 201 Created
    }

    // API 4: Cập nhật sự kiện - YÊU CẦU VAI TRÒ POSTER (là chủ sở hữu)
    // PUT http://localhost:8080/api/events/1
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id,
                                                     @RequestBody EventRequest request,
                                                     Authentication authentication) {
        String email = authentication.getName();
        EventResponse updatedEvent = eventService.updateEvent(id, request, email);
        return ResponseEntity.ok(updatedEvent);
    }

    // ...

    // API: Lấy thùng rác của tôi (Poster)
    // GET /api/events/my-trash
    @GetMapping("/my-trash")
    public ResponseEntity<List<EventResponse>> getMyTrash(Authentication authentication) {
        return ResponseEntity.ok(eventService.getMyDeletedEvents(authentication.getName()));
    }

    // API: Khôi phục sự kiện
    // POST /api/events/{id}/restore
    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restoreEvent(@PathVariable Long id, Authentication authentication) {
        eventService.restoreEvent(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    // API: Xóa vĩnh viễn
    // DELETE /api/events/{id}/permanent
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentDelete(@PathVariable Long id, Authentication authentication) {
        eventService.permanentDelete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // API 5: Xóa sự kiện - YÊU CẦU VAI TRÒ POSTER (chủ sở hữu) hoặc ADMIN
    // DELETE http://localhost:8080/api/events/1
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id,
                                            Authentication authentication) {
        String email = authentication.getName();
        eventService.deleteEvent(id, email);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // API 6: (Poster) Lấy danh sách sự kiện do TÔI tạo
    // GET http://localhost:8080/api/events/my-events
    @GetMapping("/my-events")
    public ResponseEntity<List<EventResponse>> getMyEvents(Authentication authentication) {
        String email = authentication.getName();
        List<EventResponse> events = eventService.getMyEvents(email);
        return ResponseEntity.ok(events);
    }

    // API 7: (Poster/Admin) Lấy danh sách SV tham gia
    // GET /api/events/{id}/participants
    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(@PathVariable Long id, Authentication auth) {
        List<ParticipantResponse> participants = eventService.getEventParticipants(id, auth.getName());
        return ResponseEntity.ok(participants);
    }

    // API 8: (Poster/Admin) Xuất Excel danh sách SV
    // GET /api/events/{id}/participants/export
    @GetMapping("/{id}/participants/export")
    public ResponseEntity<byte[]> exportParticipants(@PathVariable Long id, Authentication auth) {
        try {
            byte[] excelData = eventService.exportEventParticipantsToExcel(id, auth.getName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "danh-sach-tham-gia.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}