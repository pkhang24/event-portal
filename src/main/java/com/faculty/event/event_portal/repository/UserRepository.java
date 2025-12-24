package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Role;
import com.faculty.event.event_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findFirstByRole(Role role);

    List<User> findAllByRole(Role role);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByMssv(String mssv);

    // Tìm tất cả user đã bị xóa mềm
    @Query(value = "SELECT * FROM users u WHERE u.deleted_at IS NOT NULL", nativeQuery = true)
    List<User> findSoftDeleted();

    // Tìm một user đã bị xóa mềm
    @Query(value = "SELECT * FROM users u WHERE u.id = :id AND u.deleted_at IS NOT NULL", nativeQuery = true)
    Optional<User> findSoftDeletedById(@Param("id") Long id);

    // Xóa VĨNH VIỄN
    @Modifying
    @Query(value = "DELETE FROM users WHERE id = :id", nativeQuery = true)
    void permanentDelete(@Param("id") Long id);
}