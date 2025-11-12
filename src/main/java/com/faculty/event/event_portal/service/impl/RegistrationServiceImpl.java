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
import com.faculty.event.event_portal.service.RegistrationService;
import jakarta.persistence.EntityNotFoundException;
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

    public RegistrationServiceImpl(RegistrationRepository registrationRepository,
                                   UserRepository userRepository,
                                   EventRepository eventRepository) {
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
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
    @Transactional // Đảm bảo tất cả các thao tác CSDL đều thành công, hoặc rollback
    public TicketResponse createRegistration(RegistrationRequest request, String studentEmail) {

        // 1. Lấy thông tin sinh viên và sự kiện
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sinh viên"));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        // 2. KIỂM TRA LOGIC: Sinh viên đã đăng ký chưa?
        boolean alreadyRegistered = registrationRepository.existsByUserAndEvent(student, event);
        if (alreadyRegistered) {
            throw new IllegalArgumentException("Bạn đã đăng ký sự kiện này rồi");
        }

        // 3. KIỂM TRA LOGIC: Sự kiện còn chỗ không?
        if (event.getSoLuongGioiHan() != null) { // Nếu có giới hạn
            long currentRegistrations = registrationRepository.countByEvent(event);
            if (currentRegistrations >= event.getSoLuongGioiHan()) {
                throw new IllegalStateException("Sự kiện này đã hết chỗ");
            }
        }

        // 4. Mọi thứ đều ổn -> Tạo vé
        Registration newRegistration = new Registration();
        newRegistration.setUser(student);
        newRegistration.setEvent(event);
        // (Các trường khác như ticketCode, trangThai, createdAt sẽ được tự động gán bởi @PrePersist)

        // 5. Lưu vé
        Registration savedRegistration = registrationRepository.save(newRegistration);

        return convertToTicketResponse(savedRegistration);
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