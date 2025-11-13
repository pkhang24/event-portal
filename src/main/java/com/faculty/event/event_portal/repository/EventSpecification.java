package com.faculty.event.event_portal.repository; // Hoặc package khác

import com.faculty.event.event_portal.entity.Event;
import com.faculty.event.event_portal.entity.EventStatus;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;

public class EventSpecification {

    public static Specification<Event> findByCriteria(String search, String status) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            // Luôn lọc các sự kiện đã PUBLISHED
            predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(root.get("trangThai"), EventStatus.PUBLISHED));

            // 1. Lọc theo Tên (search)
            if (search != null && !search.isEmpty()) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tieuDe")), "%" + search.toLowerCase() + "%"));
            }

            // 2. Lọc theo Trạng thái (status)
            LocalDateTime now = LocalDateTime.now();
            if ("ongoing".equals(status)) {
                // Đang diễn ra: (Bắt đầu <= now) VÀ (Kết thúc >= now)
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.lessThanOrEqualTo(root.get("thoiGianBatDau"), now));
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("thoiGianKetThuc"), now));
            } else if ("upcoming".equals(status)) {
                // Sắp diễn ra: (Bắt đầu > now)
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.greaterThan(root.get("thoiGianBatDau"), now));
            }
            // (Nếu status = null, lấy tất cả)

            return predicate;
        };
    }
}