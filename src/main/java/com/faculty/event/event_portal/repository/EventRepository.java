package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Event;
import com.faculty.event.event_portal.entity.EventStatus;
import com.faculty.event.event_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification; // Import mới
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Import mới
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> { // Thêm JpaSpecificationExecutor
    // Chúng ta có thể thêm các hàm tìm kiếm tùy chỉnh sau này
    // Ví dụ: tìm sự kiện theo trạng thái, tìm sự kiện sắp diễn ra...
    // Trong file EventRepository.java
    List<Event> findAllByTrangThai(EventStatus status);

    List<Event> findAllByNguoiDang(User nguoiDang);

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

    // Thống kê số lượng sự kiện theo từng tháng trong năm (Chỉ tính PUBLISHED)
    // Trả về: [Tháng (Integer), Số lượng (Long)]
    @Query("SELECT EXTRACT(MONTH FROM e.thoiGianBatDau), COUNT(e.id) " +
            "FROM Event e " +
            "WHERE e.trangThai = com.faculty.event.event_portal.entity.EventStatus.PUBLISHED " +
            "AND EXTRACT(YEAR FROM e.thoiGianBatDau) = :year " + // Sửa chỗ này
            "GROUP BY EXTRACT(MONTH FROM e.thoiGianBatDau) " + // Sửa chỗ này
            "ORDER BY 1 ASC")
    List<Object[]> countEventsByMonth(@Param("year") int year);

    // 1. Tìm tất cả sự kiện đã bị xóa mềm (DÙNG NATIVE QUERY)
    @Query(value = "SELECT * FROM events e WHERE e.deleted_at IS NOT NULL", nativeQuery = true)
    List<Event> findSoftDeleted();

    // 2. Tìm một sự kiện đã bị xóa mềm (DÙNG NATIVE QUERY)
    @Query(value = "SELECT * FROM events e WHERE e.id = :id AND e.deleted_at IS NOT NULL", nativeQuery = true)
    Optional<Event> findSoftDeletedById(@Param("id") Long id);

    // 3. Xóa VĨNH VIỄN (DÙNG NATIVE QUERY)
    @Modifying
    @Query(value = "DELETE FROM events WHERE id = :id", nativeQuery = true)
    void permanentDelete(@Param("id") Long id);
}