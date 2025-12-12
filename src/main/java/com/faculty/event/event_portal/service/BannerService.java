package com.faculty.event.event_portal.service;

import com.faculty.event.event_portal.entity.Banner;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BannerService {
    List<Banner> getActiveBanners();
    List<Banner> getAllBanners();

    // Sửa 2 hàm này để nhận MultipartFile
    Banner createBanner(MultipartFile image, Boolean active);
    Banner updateBanner(Long id, MultipartFile image, Boolean active);

    void deleteBanner(Long id);
    Banner updateBannerStatus(Long id, boolean isActive);

    List<Banner> getDeletedBanners();
    void restoreBanner(Long id);
    void hardDeleteBanner(Long id);
}