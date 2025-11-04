package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Event;
import com.faculty.event.event_portal.entity.Registration;
import com.faculty.event.event_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    // Tìm vé theo mã vé (dùng cho điểm danh)
    Optional<Registration> findByTicketCode(String ticketCode);

    // Tìm tất cả vé của 1 sinh viên
    List<Registration> findAllByUser(User user);

    // Kiểm tra xem sinh viên đã đăng ký sự kiện này chưa
    boolean existsByUserAndEvent(User user, Event event);

    // Trong file RegistrationRepository.java
    // (Bạn đã có hàm existsByUserAndEvent và findAllByUser rồi)
    // Thêm hàm này:
    long countByEvent(Event event);
}