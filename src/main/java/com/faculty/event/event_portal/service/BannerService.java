package com.faculty.event.event_portal.service;

import com.faculty.event.event_portal.entity.Banner;
import java.util.List;

public interface BannerService {
    // --- Public ---
    List<Banner> getActiveBanners();

    // --- Admin ---
    List<Banner> getAllBanners();
    Banner createBanner(Banner banner); // Admin chỉ cần gửi JSON (imageUrl, linkUrl)
    Banner updateBanner(Long id, Banner bannerDetails);
    void deleteBanner(Long id);
    Banner updateBannerStatus(Long id, boolean isActive); // API để bật/tắt nhanh
}