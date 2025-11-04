package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Đánh dấu đây là một Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository<TênEntity, KiểuDữLiệuCủaKhóaChính>

    // Spring Data JPA sẽ tự động tạo câu lệnh SQL
    // "SELECT * FROM users WHERE email = ?"
    // chỉ bằng cách bạn đặt tên hàm là findBy[TênThuộcTính]
    Optional<User> findByEmail(String email);
}