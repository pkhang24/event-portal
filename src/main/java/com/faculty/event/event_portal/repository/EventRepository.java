package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Event;
import com.faculty.event.event_portal.entity.EventStatus;
import com.faculty.event.event_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
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
}