package com.faculty.event.event_portal.repository;

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
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByDeletedAtIsNull();

    boolean existsByTenDanhMucIgnoreCase(String tenDanhMuc);

    // Đếm số sự kiện thuộc danh mục này
    @Query("SELECT COUNT(e) FROM Event e WHERE e.category = :category AND e.deletedAt IS NULL")
    Long countEventsByCategory(@Param("category") Category category);

    // Tìm thùng rác
    @Query(value = "SELECT * FROM categories WHERE deleted_at IS NOT NULL", nativeQuery = true)
    List<Category> findSoftDeleted();

    // Tìm 1 mục trong thùng rác
    @Query(value = "SELECT * FROM categories WHERE id = :id AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<Category> findSoftDeletedById(@Param("id") Long id);

    // Khôi phục
    @Modifying
    @Transactional
    @Query(value = "UPDATE categories SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restoreCategory(@Param("id") Long id);

    // Xóa VĨNH VIỄN
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM categories WHERE id = :id", nativeQuery = true)
    void permanentDelete(@Param("id") Long id);
}