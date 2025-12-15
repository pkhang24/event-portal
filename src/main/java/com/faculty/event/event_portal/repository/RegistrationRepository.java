package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Event;
import com.faculty.event.event_portal.entity.Registration;
import com.faculty.event.event_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    // Tìm vé theo mã vé (dùng cho điểm danh)
    Optional<Registration> findByTicketCode(String ticketCode);

    // Tìm tất cả vé của 1 sinh viên
    List<Registration> findAllByUser(User user);

    List<Registration> findAllByEvent(Event event);

    // Kiểm tra xem sinh viên đã đăng ký sự kiện này chưa
    boolean existsByUserAndEvent(User user, Event event);

    // Trong file RegistrationRepository.java
    // (Bạn đã có hàm existsByUserAndEvent và findAllByUser rồi)
    // Thêm hàm này:
    long countByEvent(Event event);

    // Thêm hàm xóa tất cả lượt đăng ký của 1 user
    // @Transactional và @Modifying là bắt buộc cho lệnh DELETE/UPDATE tùy chỉnh
    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM Registration r WHERE r.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    // Xóa tất cả vé của một sự kiện
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM registrations WHERE event_id = :eventId", nativeQuery = true)
    void deleteRegistrationsByEventId(@Param("eventId") Long eventId);

    // Lấy lịch sử: vé đã ATTENDED và sự kiện đã KẾT THÚC (thoiGianKetThuc < now)
    @Query("SELECT r FROM Registration r " +
            "WHERE r.user = :user " +
            "AND r.trangThai = com.faculty.event.event_portal.entity.RegistrationStatus.ATTENDED " +
            "AND r.event.thoiGianKetThuc < :now")
    List<Registration> findHistoryByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    // Lấy tất cả các vé (Registrations) của sự kiện CHƯA KẾT THÚC
    @Query("SELECT r FROM Registration r " +
            "WHERE r.user = :user " +
            "AND r.event.thoiGianKetThuc > :now")
    List<Registration> findActiveRegistrationsByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    // Thống kê tỉ lệ tham gia theo chủ đề TRONG KHOẢNG THỜI GIAN
    @Query("SELECT c.tenDanhMuc, COUNT(r.id) " +
            "FROM Registration r " +
            "JOIN r.event e " +
            "JOIN e.category c " +
            "WHERE e.thoiGianBatDau BETWEEN :startDate AND :endDate " + // <<< THÊM DÒNG NÀY
            "GROUP BY c.id, c.tenDanhMuc " +
            "ORDER BY COUNT(r.id) DESC")
    List<Object[]> findCategoryStatsInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}