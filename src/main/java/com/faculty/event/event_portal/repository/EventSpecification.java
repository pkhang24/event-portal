package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Event;
import com.faculty.event.event_portal.entity.EventStatus;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;

public class EventSpecification {

    public static Specification<Event> findByCriteria(String search, String status, Long categoryId) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(root.get("trangThai"), EventStatus.PUBLISHED));

            if (search != null && !search.isEmpty()) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tieuDe")), "%" + search.toLowerCase() + "%"));
            }

            LocalDateTime now = LocalDateTime.now();
            if ("ongoing".equals(status)) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.lessThanOrEqualTo(root.get("thoiGianBatDau"), now));
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("thoiGianKetThuc"), now));
            } else if ("upcoming".equals(status)) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.greaterThan(root.get("thoiGianBatDau"), now));
            }

            if (categoryId != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            return predicate;
        };
    }
}