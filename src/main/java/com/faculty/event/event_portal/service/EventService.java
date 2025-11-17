package com.faculty.event.event_portal.service;

import com.faculty.event.event_portal.dto.EventRequest;
import com.faculty.event.event_portal.dto.EventResponse;
import com.faculty.event.event_portal.dto.ParticipantResponse;
import java.io.IOException;

import java.util.List;

public interface EventService {

    // Lấy tất cả sự kiện (public, đã PUBLISHED)
    List<EventResponse> getAllPublishedEvents(String search, String status, Long categoryId);

    List<EventResponse> getMyEvents(String posterEmail);

    // Lấy chi tiết 1 sự kiện (public)
    EventResponse getEventById(Long id);

    // Tạo sự kiện mới (cho POSTER)
    EventResponse createEvent(EventRequest request, String posterEmail);

    // Cập nhật sự kiện (cho POSTER)
    EventResponse updateEvent(Long id, EventRequest request, String posterEmail);

    // Xóa sự kiện (cho POSTER/ADMIN)
    void deleteEvent(Long id, String userEmail);

    List<ParticipantResponse> getEventParticipants(Long eventId, String posterEmail);
    byte[] exportEventParticipantsToExcel(Long eventId, String posterEmail) throws IOException; // Dùng để trả về file Excel
}