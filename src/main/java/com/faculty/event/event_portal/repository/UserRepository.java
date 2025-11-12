package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository // Đánh dấu đây là một Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository<TênEntity, KiểuDữLiệuCủaKhóaChính>

    // Spring Data JPA sẽ tự động tạo câu lệnh SQL
    // "SELECT * FROM users WHERE email = ?"
    // chỉ bằng cách bạn đặt tên hàm là findBy[TênThuộcTính]
    Optional<User> findByEmail(String email);

    // 1. Tìm tất cả user đã bị xóa mềm
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL")
    List<User> findSoftDeleted();

    // 2. Tìm một user đã bị xóa mềm
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NOT NULL")
    Optional<User> findSoftDeletedById(@Param("id") Long id);

    // 3. Xóa VĨNH VIỄN (bỏ qua @SQLDelete)
    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :id")
    void permanentDelete(@Param("id") Long id);
}