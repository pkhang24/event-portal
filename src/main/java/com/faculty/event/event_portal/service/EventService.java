package com.faculty.event.event_portal.service;

import com.faculty.event.event_portal.dto.EventRequest;
import com.faculty.event.event_portal.dto.EventResponse;
import com.faculty.event.event_portal.dto.ParticipantResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;

public interface EventService {

    // Lấy tất cả sự kiện (public, đã PUBLISHED)
    List<EventResponse> getAllPublishedEvents(String search, String status, Long categoryId);

    List<EventResponse> getAllEventsForAdmin();

    List<EventResponse> getMyEvents(String posterEmail);

    // Lấy chi tiết 1 sự kiện
    EventResponse getEventById(Long id);

    EventResponse createEvent(EventRequest request, MultipartFile image, MultipartFile coverImage, String posterEmail);

    EventResponse updateEvent(Long id, EventRequest request, MultipartFile image, MultipartFile coverImage, String posterEmail);

    // Xóa sự kiện
    void deleteEvent(Long id, String userEmail);

    void rejectEvent(Long EventId, String reason);

    void cancelEvent(Long EventId, String reason);

    // Lấy danh sách các sự kiện đã xóa của Poster
    List<EventResponse> getMyDeletedEvents(String posterEmail);

    // Khôi phục sự kiện từ thùng rác
    void restoreEvent(Long id, String userEmail);

    // Xóa vĩnh viễn sự kiện
    void permanentDelete(Long id, String userEmail);

    List<ParticipantResponse> getEventParticipants(Long eventId, String posterEmail);
    byte[] exportEventParticipantsToExcel(Long eventId, String posterEmail) throws IOException; // Dùng để trả về file Excel
}