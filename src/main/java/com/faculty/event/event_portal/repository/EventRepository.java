package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Event;
import com.faculty.event.event_portal.entity.EventStatus;
import com.faculty.event.event_portal.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> { // Thêm JpaSpecificationExecutor
    List<Event> findAllByTrangThai(EventStatus status);

    List<Event> findAllByTrangThaiNot(EventStatus status, Sort sort);

    List<Event> findAllByNguoiDang(User nguoiDang);

    @Query(value = "SELECT * FROM events WHERE nguoi_dang_id = :userId", nativeQuery = true)
    List<Event> findAllByNguoiDangIdIncludingDeleted(@Param("userId") Long userId);

    List<Event> findAllByCategoryId(Long categoryId);

    // Lấy Top 5 sự kiện có nhiều lượt đăng ký nhất
    @Query("SELECT e.tieuDe, COUNT(r.id) as luotDangKy " +
            "FROM Event e JOIN Registration r ON e.id = r.event.id " +
            "GROUP BY e.id " +
            "ORDER BY luotDangKy DESC " +
            "LIMIT 5")
    List<Object[]> findTop5EventsByRegistration();

    // Lấy Top sự kiện (đã PUBLISHED) có nhiều lượt đăng ký nhất trong một khoảng thời gian
    @Query("SELECT e.tieuDe, COUNT(r.id) as luotDangKy " +
            "FROM Event e " +
            "JOIN Registration r ON e.id = r.event.id " +
            "WHERE e.trangThai = com.faculty.event.event_portal.entity.EventStatus.PUBLISHED " +
            "AND e.thoiGianBatDau BETWEEN :startDate AND :endDate " +
            "GROUP BY e.id " +
            "ORDER BY luotDangKy DESC")
    List<Object[]> findTopEventsByRegistrationInDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Thống kê số lượng sự kiện theo từng tháng trong năm
    @Query("SELECT EXTRACT(MONTH FROM e.thoiGianBatDau), COUNT(e.id) " +
            "FROM Event e " +
            "WHERE e.trangThai = com.faculty.event.event_portal.entity.EventStatus.PUBLISHED " +
            "AND EXTRACT(YEAR FROM e.thoiGianBatDau) = :year " +
            "GROUP BY EXTRACT(MONTH FROM e.thoiGianBatDau) " +
            "ORDER BY 1 ASC")
    List<Object[]> countEventsByMonth(@Param("year") int year);

    // Tìm tất cả sự kiện đã bị xóa mềm
    @Query(value = "SELECT * FROM events e WHERE e.deleted_at IS NOT NULL", nativeQuery = true)
    List<Event> findSoftDeleted();

    // ìm một sự kiện đã bị xóa mềm
    @Query(value = "SELECT * FROM events e WHERE e.id = :id AND e.deleted_at IS NOT NULL", nativeQuery = true)
    Optional<Event> findSoftDeletedById(@Param("id") Long id);

    // HÀM CHO POSTER: Tìm sự kiện đã xóa của riêng Poster
    @Query(value = "SELECT * FROM events e WHERE e.deleted_at IS NOT NULL AND e.nguoi_dang_id = :userId", nativeQuery = true)
    List<Event> findSoftDeletedByUserId(@Param("userId") Long userId);

    // Tìm 1 sự kiện đã xóa
    @Query(value = "SELECT * FROM events e WHERE e.id = :id AND e.deleted_at IS NOT NULL", nativeQuery = true)
    Optional<Event> findDeletedById(@Param("id") Long id);

    // Khôi phục sự kiện
    @Modifying
    @Query(value = "UPDATE events SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restoreEvent(@Param("id") Long id);

    // Xóa sạch các vé đăng ký trước khi xóa sự kiện (để tránh lỗi FK)
    @Modifying
    @Query(value = "DELETE FROM registrations WHERE event_id = :eventId", nativeQuery = true)
    void deleteRegistrationsByEventId(@Param("eventId") Long eventId);

    // Xóa VĨNH VIỄN (DÙNG NATIVE QUERY)
    @Modifying
    @Query(value = "DELETE FROM events WHERE id = :id", nativeQuery = true)
    void permanentDelete(@Param("id") Long id);

    // Gỡ liên kết đến danh mục của sự kiện
    @Modifying
    @Transactional
    @Query(value = "UPDATE events SET category_id = NULL WHERE category_id = :categoryId", nativeQuery = true)
    void unlinkCategory(@Param("categoryId") Long categoryId);
}