package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.dto.EventRequest;
import com.faculty.event.event_portal.dto.EventResponse;
import com.faculty.event.event_portal.entity.Event;
import com.faculty.event.event_portal.entity.EventStatus;
import com.faculty.event.event_portal.entity.Role;
import com.faculty.event.event_portal.entity.User;
import com.faculty.event.event_portal.repository.EventRepository;
import com.faculty.event.event_portal.repository.UserRepository;
import com.faculty.event.event_portal.service.EventService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventServiceImpl(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    // --- Hàm helper để chuyển Entity -> DTO ---
    private EventResponse convertToResponse(Event event) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setTieuDe(event.getTieuDe());
        response.setMoTaNgan(event.getMoTaNgan());
        response.setNoiDung(event.getNoiDung());
        response.setAnhThumbnail(event.getAnhThumbnail());
        response.setThoiGianBatDau(event.getThoiGianBatDau());
        response.setThoiGianKetThuc(event.getThoiGianKetThuc());
        response.setDiaDiem(event.getDiaDiem());
        response.setSoLuongGioiHan(event.getSoLuongGioiHan());
        response.setTrangThai(event.getTrangThai().name());
        response.setLuotXem(event.getLuotXem());
        response.setTenNguoiDang(event.getNguoiDang().getHoTen());
        response.setCreatedAt(event.getCreatedAt());
        return response;
    }

    @Override
    public List<EventResponse> getAllPublishedEvents() {
        // Chỉ lấy các sự kiện đã PUBLISHED và map sang DTO
        return eventRepository.findAllByTrangThai(EventStatus.PUBLISHED)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        // Tăng lượt xem (có thể cải tiến sau)
        event.setLuotXem(event.getLuotXem() + 1);
        eventRepository.save(event);

        return convertToResponse(event);
    }

    @Override
    public EventResponse createEvent(EventRequest request, String posterEmail) {
        // 1. Tìm user (người đăng)
        User poster = userRepository.findByEmail(posterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người đăng"));

        // 2. Chuyển DTO -> Entity
        Event event = new Event();
        event.setTieuDe(request.getTieuDe());
        event.setMoTaNgan(request.getMoTaNgan());
        event.setNoiDung(request.getNoiDung());
        event.setAnhThumbnail(request.getAnhThumbnail());
        event.setThoiGianBatDau(request.getThoiGianBatDau());
        event.setThoiGianKetThuc(request.getThoiGianKetThuc());
        event.setDiaDiem(request.getDiaDiem());
        event.setSoLuongGioiHan(request.getSoLuongGioiHan());
        event.setNguoiDang(poster); // Gán người tạo
        event.setTrangThai(EventStatus.DRAFT); // Mặc định là bản nháp, ADMIN sẽ duyệt sau

        // 3. Lưu vào CSDL
        Event savedEvent = eventRepository.save(event);

        return convertToResponse(savedEvent);
    }

    @Override
    public EventResponse updateEvent(Long id, EventRequest request, String posterEmail) {
        // 1. Tìm sự kiện
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        // 2. Tìm người dùng
        User poster = userRepository.findByEmail(posterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        // 3. Kiểm tra quyền: Chỉ chủ sở hữu mới được sửa
        if (!event.getNguoiDang().getId().equals(poster.getId())) {
            throw new AccessDeniedException("Bạn không có quyền sửa sự kiện này");
        }

        // 4. Cập nhật thông tin
        event.setTieuDe(request.getTieuDe());
        event.setMoTaNgan(request.getMoTaNgan());
        event.setNoiDung(request.getNoiDung());
        event.setAnhThumbnail(request.getAnhThumbnail());
        event.setThoiGianBatDau(request.getThoiGianBatDau());
        event.setThoiGianKetThuc(request.getThoiGianKetThuc());
        event.setDiaDiem(request.getDiaDiem());
        event.setSoLuongGioiHan(request.getSoLuongGioiHan());

        Event updatedEvent = eventRepository.save(event);
        return convertToResponse(updatedEvent);
    }

    @Override
    public void deleteEvent(Long id, String userEmail) {
        // 1. Tìm sự kiện
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        // 2. Tìm người dùng
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        // 3. Kiểm tra quyền: Chủ sở hữu HOẶC ADMIN mới được xóa
        if (!event.getNguoiDang().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xóa sự kiện này");
        }

        // (Cần kiểm tra thêm: nếu đã có người đăng ký thì không cho xóa, mà chỉ nên "hủy")
        // Tạm thời chúng ta cho phép xóa
        eventRepository.delete(event);
    }
}