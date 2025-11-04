package com.faculty.event.event_portal.repository;

import com.faculty.event.event_portal.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // Lấy tất cả banner đang hoạt động
    List<Banner> findAllByIsActiveTrue();
}