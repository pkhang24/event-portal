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

    // 1. Tìm tất cả sự kiện đã bị xóa mềm
    @Query("SELECT e FROM Event e WHERE e.deletedAt IS NOT NULL")
    List<Event> findSoftDeleted();

    // 2. Tìm một sự kiện đã bị xóa mềm
    @Query("SELECT e FROM Event e WHERE e.id = :id AND e.deletedAt IS NOT NULL")
    Optional<Event> findSoftDeletedById(@Param("id") Long id);

    // 3. Xóa VĨNH VIỄN (bỏ qua @SQLDelete)
    @Modifying
    @Query("DELETE FROM Event e WHERE e.id = :id")
    void permanentDelete(@Param("id") Long id);
}