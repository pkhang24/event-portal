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

    // Hàm helper để chuyển Entity sang DTO
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

        // Lấy thông tin
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sinh viên"));

        Event eventMoi = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        // Đã đăng ký sự kiện này chưa
        boolean alreadyRegistered = registrationRepository.existsByUserAndEvent(student, eventMoi);
        if (alreadyRegistered) {
            throw new IllegalArgumentException("Bạn đã đăng ký sự kiện này rồi");
        }

        // Sự kiện còn chỗ không
        if (eventMoi.getSoLuongGioiHan() != null) {
            // Đếm số lượng vé ĐÃ ĐĂNG KÝ THÀNH CÔNG của sự kiện này
            long soLuongDaDangKy = registrationRepository.countByEventIdAndTrangThai(
                    eventMoi.getId(),
                    RegistrationStatus.ATTENDED
            );

            // So sánh với giới hạn
            if (soLuongDaDangKy >= eventMoi.getSoLuongGioiHan()) {
                throw new RuntimeException("Sự kiện đã hết chỗ, vui lòng quay lại sau!");
            }
        }

        // Trùng lặp thời gian
        LocalDateTime now = LocalDateTime.now();
        // Lấy tất cả các vé MÀ SINH VIÊN ĐÃ ĐĂNG KÝ
        List<Registration> cacVeDaDangKy = registrationRepository.findActiveRegistrationsByUser(student, now);

        for (Registration ve : cacVeDaDangKy) {
            Event eventDaDangKy = ve.getEvent();

            // Công thức kiểm tra 2 khoảng thời gian bị overlap (trùng lặp)
            // (A.Start < B.End) AND (A.End > B.Start)
            boolean isOverlapping =
                    eventMoi.getThoiGianBatDau().isBefore(eventDaDangKy.getThoiGianKetThuc()) &&
                            eventMoi.getThoiGianKetThuc().isAfter(eventDaDangKy.getThoiGianBatDau());

            if (isOverlapping) {
                throw new IllegalStateException("Bạn đã đăng ký sự kiện \"" + eventDaDangKy.getTieuDe() + "\" bị trùng thời gian.");
            }

            Registration newRegistration = new Registration();
            newRegistration.setUser(student);
            newRegistration.setEvent(eventMoi);

            Registration savedRegistration = registrationRepository.save(newRegistration);

            try {
                notificationService.createNotification(
                        student,
                        "Đăng ký thành công",
                        "Bạn đã đăng ký thành công sự kiện: " + eventMoi.getTieuDe(),
                        "SUCCESS"
                );

                if (eventMoi.getNguoiDang() != null) {
                    notificationService.createNotification(
                            eventMoi.getNguoiDang(),
                            "Có người đăng ký mới",
                            student.getHoTen() + " vừa đăng ký sự kiện của bạn.",
                            "INFO"
                    );
                }
            } catch (Exception e) {
                System.err.println("Lỗi tạo thông báo: " + e.getMessage());
            }

            return convertToTicketResponse(savedRegistration);
        }

        Registration newRegistration = new Registration();
        newRegistration.setUser(student);
        newRegistration.setEvent(eventMoi);

        Registration savedRegistration = registrationRepository.save(newRegistration);

        return convertToTicketResponse(savedRegistration);
    }

    @Override
    @Transactional
    public void cancelRegistration(Long registrationId, String studentEmail) {
        // Tìm vé
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy vé đăng ký"));

        // Kiểm tra xem user có phải chủ vé không
        if (!registration.getUser().getEmail().equals(studentEmail)) {
            throw new AccessDeniedException("Bạn không có quyền hủy vé này");
        }

        // Kiểm tra xem sự kiện đã bắt đầu chưa
        if (registration.getEvent().getThoiGianBatDau().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Không thể hủy vé vì sự kiện đã diễn ra");
        }

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
        // Tìm vé bằng mã code (phải là unique)
        Registration registration = registrationRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new EntityNotFoundException("Mã vé không hợp lệ"));

        // Kiểm tra xem vé đã được điểm danh chưa
        if (registration.getTrangThai() == RegistrationStatus.ATTENDED) {
            throw new IllegalStateException("Vé này đã được điểm danh rồi");
        }

        // Cập nhật trạng thái
        registration.setTrangThai(RegistrationStatus.ATTENDED);

        Registration updatedRegistration = registrationRepository.save(registration);

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

        return convertToTicketResponse(updatedRegistration);
    }

    @Override
    public List<TicketResponse> getMyHistory(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sinh viên"));

        List<Registration> historyRegistrations = registrationRepository.findHistoryByUser(student, LocalDateTime.now());

        return historyRegistrations.stream()
                .map(this::convertToTicketResponse)
                .collect(Collectors.toList());
    }
}