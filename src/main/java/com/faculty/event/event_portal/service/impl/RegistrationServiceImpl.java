package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.dto.RegistrationRequest;
import com.faculty.event.event_portal.dto.TicketResponse;
import com.faculty.event.event_portal.entity.Event;
import com.faculty.event.event_portal.entity.Registration;
import com.faculty.event.event_portal.entity.RegistrationStatus;
import com.faculty.event.event_portal.entity.User;
import com.faculty.event.event_portal.repository.EventRepository;
import com.faculty.event.event_portal.repository.RegistrationRepository;
import com.faculty.event.event_portal.repository.UserRepository;
import com.faculty.event.event_portal.service.NotificationService;
import com.faculty.event.event_portal.service.RegistrationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;

    public RegistrationServiceImpl(RegistrationRepository registrationRepository,
                                   UserRepository userRepository,
                                   EventRepository eventRepository,
                                   NotificationService notificationService) {
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
    }

    // --- Hàm helper để chuyển Entity -> DTO ---
    private TicketResponse convertToTicketResponse(Registration reg) {
        TicketResponse ticket = new TicketResponse();
        ticket.setRegistrationId(reg.getId());
        ticket.setTicketCode(reg.getTicketCode());
        ticket.setTrangThai(reg.getTrangThai().name());

        ticket.setEventId(reg.getEvent().getId());
        ticket.setTieuDeSuKien(reg.getEvent().getTieuDe());
        ticket.setThoiGianBatDau(reg.getEvent().getThoiGianBatDau());
        ticket.setDiaDiem(reg.getEvent().getDiaDiem());

        ticket.setStudentId(reg.getUser().getId());
        ticket.setTenSinhVien(reg.getUser().getHoTen());

        return ticket;
    }

    @Override
    @Transactional
    public TicketResponse createRegistration(RegistrationRequest request, String studentEmail) {

        // 1. Lấy thông tin (Giữ nguyên)
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sinh viên"));

        Event eventMoi = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        // 2. KIỂM TRA LOGIC 1: Đã đăng ký sự kiện này chưa? (Giữ nguyên)
        boolean alreadyRegistered = registrationRepository.existsByUserAndEvent(student, eventMoi);
        if (alreadyRegistered) {
            throw new IllegalArgumentException("Bạn đã đăng ký sự kiện này rồi");
        }

        // 3. KIỂM TRA LOGIC 2: Sự kiện còn chỗ không? (Giữ nguyên)
        if (eventMoi.getSoLuongGioiHan() != null) {
            // ... (code kiểm tra số lượng)
        }

        // === 4. KIỂM TRA LOGIC MỚI: Trùng lặp thời gian ===
        LocalDateTime now = LocalDateTime.now();
        // Lấy tất cả các vé MÀ SINH VIÊN ĐÃ ĐĂNG KÝ (cho các sự kiện chưa kết thúc)
        List<Registration> cacVeDaDangKy = registrationRepository.findActiveRegistrationsByUser(student, now);

        for (Registration ve : cacVeDaDangKy) {
            Event eventDaDangKy = ve.getEvent();

            // Công thức kiểm tra 2 khoảng thời gian bị overlap (trùng lặp)
            // (A.Start < B.End) AND (A.End > B.Start)
            boolean isOverlapping =
                    eventMoi.getThoiGianBatDau().isBefore(eventDaDangKy.getThoiGianKetThuc()) &&
                            eventMoi.getThoiGianKetThuc().isAfter(eventDaDangKy.getThoiGianBatDau());

            if (isOverlapping) {
                // Nếu trùng, ném lỗi và dừng lại
                throw new IllegalStateException("Bạn đã đăng ký sự kiện \"" + eventDaDangKy.getTieuDe() + "\" bị trùng thời gian.");
            }

            // === KHẮC PHỤC LỖI Ở ĐÂY: KHỞI TẠO BIẾN newRegistration ===
            Registration newRegistration = new Registration();
            newRegistration.setUser(student);
            newRegistration.setEvent(eventMoi);
            // Nếu bạn có logic sinh mã vé, hãy thêm vào đây. Ví dụ:
            // newRegistration.setTicketCode(UUID.randomUUID().toString());

            // Sau đó mới đến dòng lưu (Dòng đang bị lỗi của bạn)
            Registration savedRegistration = registrationRepository.save(newRegistration);
            // ==========================================================

            // === TẠO THÔNG BÁO (Notification) ===
            try {
                // Thông báo cho Sinh viên
                notificationService.createNotification(
                        student,
                        "Đăng ký thành công",
                        "Bạn đã đăng ký thành công sự kiện: " + eventMoi.getTieuDe(),
                        "SUCCESS"
                );

                // Thông báo cho Người đăng (Poster)
                // Kiểm tra null để tránh lỗi nếu người đăng đã bị xóa
                if (eventMoi.getNguoiDang() != null) {
                    notificationService.createNotification(
                            eventMoi.getNguoiDang(),
                            "Có người đăng ký mới",
                            student.getHoTen() + " vừa đăng ký sự kiện của bạn.",
                            "INFO"
                    );
                }
            } catch (Exception e) {
                // Log lỗi nhưng không chặn việc đăng ký
                System.err.println("Lỗi tạo thông báo: " + e.getMessage());
            }

            return convertToTicketResponse(savedRegistration);
        }
        // =================================================

        // 5. Mọi thứ đều ổn -> Tạo vé (Giữ nguyên)
        Registration newRegistration = new Registration();
        newRegistration.setUser(student);
        newRegistration.setEvent(eventMoi);

        Registration savedRegistration = registrationRepository.save(newRegistration);

        return convertToTicketResponse(savedRegistration);
    }

    @Override
    @Transactional
    public void cancelRegistration(Long registrationId, String studentEmail) {
        // 1. Tìm vé
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy vé đăng ký"));

        // 2. Kiểm tra xem user có phải chủ vé không
        if (!registration.getUser().getEmail().equals(studentEmail)) {
            throw new AccessDeniedException("Bạn không có quyền hủy vé này");
        }

        // 3. Kiểm tra xem sự kiện đã bắt đầu chưa
        if (registration.getEvent().getThoiGianBatDau().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Không thể hủy vé vì sự kiện đã diễn ra");
        }

        // 4. Mọi thứ OK -> Xóa vé
        registrationRepository.delete(registration);
    }

    @Override
    public List<TicketResponse> getMyTickets(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sinh viên"));

        // Tìm tất cả vé của sinh viên này
        return registrationRepository.findAllByUser(student)
                .stream()
                .map(this::convertToTicketResponse) // Chuyển từng vé sang DTO
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TicketResponse checkInTicket(String ticketCode) {
        // 1. Tìm vé bằng mã code (phải là unique)
        Registration registration = registrationRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new EntityNotFoundException("Mã vé không hợp lệ"));

        // 2. Kiểm tra xem vé đã được điểm danh chưa
        if (registration.getTrangThai() == RegistrationStatus.ATTENDED) {
            throw new IllegalStateException("Vé này đã được điểm danh rồi");
        }

        // 3. Cập nhật trạng thái
        registration.setTrangThai(RegistrationStatus.ATTENDED);

        // 4. Lưu lại
        Registration updatedRegistration = registrationRepository.save(registration);

        // === THÊM ĐOẠN NÀY: Báo cho Sinh viên ===
        try {
            notificationService.createNotification(
                    registration.getUser(),
                    "Điểm danh thành công",
                    "Bạn đã điểm danh thành công tại sự kiện: " + registration.getEvent().getTieuDe(),
                    "SUCCESS"
            );
        } catch (Exception e) {
            System.err.println("Lỗi thông báo checkin: " + e.getMessage());
        }

        // 5. Trả về thông tin vé (để hiển thị cho người check-in)
        return convertToTicketResponse(updatedRegistration);
    }

    @Override
    public List<TicketResponse> getMyHistory(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sinh viên"));

        // Gọi hàm repository mới
        List<Registration> historyRegistrations = registrationRepository.findHistoryByUser(student, LocalDateTime.now());

        return historyRegistrations.stream()
                .map(this::convertToTicketResponse) // Dùng lại hàm convert đã có
                .collect(Collectors.toList());
    }
}