package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // Lấy banner cho trang chủ (Active + Chưa xóa)
    @Query("SELECT b FROM Banner b WHERE b.isActive = true AND b.deletedAt IS NULL ORDER BY b.createdAt DESC")
    List<Banner> findAllActive();

    // Lấy tất cả banner CHƯA XÓA (cho Admin)
    @Query("SELECT b FROM Banner b WHERE b.deletedAt IS NULL ORDER BY b.createdAt DESC")
    List<Banner> findAllNotDeleted();

    // Tìm banner trong thùng rác
    @Query(value = "SELECT * FROM banners WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", nativeQuery = true)
    List<Banner> findSoftDeleted();

    // Tìm 1 banner trong thùng rác
    @Query(value = "SELECT * FROM banners WHERE id = :id AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<Banner> findSoftDeletedById(@Param("id") Long id);
}