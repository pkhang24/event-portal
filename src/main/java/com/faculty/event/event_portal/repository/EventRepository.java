package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Event;
import com.faculty.event.event_portal.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    // Chúng ta có thể thêm các hàm tìm kiếm tùy chỉnh sau này
    // Ví dụ: tìm sự kiện theo trạng thái, tìm sự kiện sắp diễn ra...
    // Trong file EventRepository.java
    List<Event> findAllByTrangThai(EventStatus status);
}