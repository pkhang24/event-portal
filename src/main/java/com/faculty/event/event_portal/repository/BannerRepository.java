package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Banner;
import com.faculty.event.event_portal.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {
    // Lấy tất cả banner đang hoạt động
    List<Banner> findAllByIsActiveTrue();

    List<Banner> findAllByDeletedAtIsNotNull();

    @Query(value = "SELECT * FROM banners WHERE id = ?1", nativeQuery = true)
    Optional<Banner> findDeletedById(Long id);

    // 1. Tìm thùng rác
    @Query(value = "SELECT * FROM banners WHERE deleted_at IS NOT NULL", nativeQuery = true)
    List<Banner> findSoftDeleted();

    // 2. Tìm 1 mục
    @Query(value = "SELECT * FROM banners WHERE id = :id AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<Banner> findSoftDeletedById(@Param("id") Long id);

    // 3. Khôi phục
    @Modifying
    @Transactional
    @Query(value = "UPDATE banners SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restoreBanner(@Param("id") Long id);

    // 4. Xóa VĨNH VIỄN
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM banners WHERE id = :id", nativeQuery = true)
    void permanentDelete(@Param("id") Long id);
}