package com.faculty.event.event_portal.controller;

import com.faculty.event.event_portal.dto.EventRequest;
import com.faculty.event.event_portal.dto.EventResponse;
import com.faculty.event.event_portal.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<EventResponse>> getAllPublishedEvents() {
        List<EventResponse> events = eventService.getAllPublishedEvents();
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
}