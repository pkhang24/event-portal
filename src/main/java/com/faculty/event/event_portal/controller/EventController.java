package com.faculty.event.event_portal.controller;

import com.faculty.event.event_portal.dto.EventRequest;
import com.faculty.event.event_portal.dto.EventResponse;
import com.faculty.event.event_portal.dto.ParticipantResponse;
import com.faculty.event.event_portal.service.EventService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // 1. Lấy danh sách public (Trang chủ)
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllPublishedEvents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId
    ) {
        List<EventResponse> events = eventService.getAllPublishedEvents(search, status, categoryId);
        return ResponseEntity.ok(events);
    }

    // 2. Chi tiết sự kiện
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        EventResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    // 3. Tạo sự kiện (Poster)
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<EventResponse> createEvent(
            @ModelAttribute EventRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            Authentication authentication) {
        String email = authentication.getName();
        EventResponse createdEvent = eventService.createEvent(request, image, coverImage, email);
        return ResponseEntity.status(201).body(createdEvent);
    }

    // 4. Cập nhật sự kiện (Poster)
    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @ModelAttribute EventRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            Authentication authentication) {
        String email = authentication.getName();
        EventResponse updatedEvent = eventService.updateEvent(id, request, image, coverImage, email);
        return ResponseEntity.ok(updatedEvent);
    }

    // 5. [QUAN TRỌNG] Lấy danh sách sự kiện do TÔI tạo (Poster)
    @GetMapping("/my-events")
    public ResponseEntity<List<EventResponse>> getMyEvents(Authentication authentication) {
        String email = authentication.getName();
        List<EventResponse> events = eventService.getMyEvents(email);
        return ResponseEntity.ok(events);
    }

    // 6. Xóa mềm sự kiện - YÊU CẦU VAI TRÒ POSTER (chủ sở hữu) hoặc ADMIN
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        eventService.deleteEvent(id, email);
        return ResponseEntity.noContent().build();
    }

    // 7. Lấy thùng rác của tôi (Poster)
    @GetMapping("/my-trash")
    public ResponseEntity<List<EventResponse>> getMyTrash(Authentication authentication) {
        return ResponseEntity.ok(eventService.getMyDeletedEvents(authentication.getName()));
    }

    // 8. Khôi phục sự kiện (Poster)
    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restoreEvent(@PathVariable Long id, Authentication authentication) {
        eventService.restoreEvent(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    // 9. Xóa vĩnh viễn (Poster)
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentDelete(@PathVariable Long id, Authentication authentication) {
        eventService.permanentDelete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // 10. Lấy danh sách SV tham gia (Cho Poster/Admin xem)
    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(@PathVariable Long id, Authentication auth) {
        List<ParticipantResponse> participants = eventService.getEventParticipants(id, auth.getName());
        return ResponseEntity.ok(participants);
    }

    // 11. Xuất Excel danh sách SV (Cho Poster/Admin)
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