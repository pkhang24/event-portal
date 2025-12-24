package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Event;
import com.faculty.event.event_portal.entity.Registration;
import com.faculty.event.event_portal.entity.RegistrationStatus;
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

    Optional<Registration> findByTicketCode(String ticketCode);

    List<Registration> findAllByUser(User user);

    List<Registration> findAllByEvent(Event event);

    boolean existsByUserAndEvent(User user, Event event);

    long countByEvent(Event event);

//    long countByEventIdAndTrangThai(Long id, String attended);
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId AND r.trangThai = :trangThai")
    long countByEventIdAndTrangThai(@Param("eventId") Long eventId, @Param("trangThai") RegistrationStatus trangThai);

    // Xóa tất cả lượt đăng ký của 1 user
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
            "WHERE e.thoiGianBatDau BETWEEN :startDate AND :endDate " +
            "GROUP BY c.id, c.tenDanhMuc " +
            "ORDER BY COUNT(r.id) DESC")
    List<Object[]> findCategoryStatsInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}