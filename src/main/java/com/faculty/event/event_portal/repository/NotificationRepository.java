package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Notification;
import com.faculty.event.event_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Lấy danh sách thông báo của user, mới nhất lên đầu
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // Đếm số thông báo chưa đọc
    long countByUserAndIsReadFalse(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}