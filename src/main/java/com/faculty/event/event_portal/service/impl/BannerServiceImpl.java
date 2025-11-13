package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.entity.Banner;
import com.faculty.event.event_portal.repository.BannerRepository;
import com.faculty.event.event_portal.service.BannerService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    public BannerServiceImpl(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    // --- Public ---
    @Override
    public List<Banner> getActiveBanners() {
        // Hàm này bạn đã định nghĩa trong Repository rồi
        return bannerRepository.findAllByIsActiveTrue();
    }

    // --- Admin ---
    @Override
    public List<Banner> getAllBanners() {
        return bannerRepository.findAll();
    }

    @Override
    public Banner createBanner(Banner banner) {
        // Gán giá trị mặc định nếu cần
        banner.setActive(true);
        return bannerRepository.save(banner);
    }

    @Override
    public Banner updateBanner(Long id, Banner bannerDetails) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Banner"));

        banner.setImageUrl(bannerDetails.getImageUrl());
        banner.setLinkUrl(bannerDetails.getLinkUrl());
        banner.setActive(bannerDetails.isActive()); // Cập nhật cả trạng thái

        return bannerRepository.save(banner);
    }

    @Override
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Banner"));
        bannerRepository.delete(banner);
        // Banner không cần soft-delete, xóa vĩnh viễn là được
    }

    @Override
    public Banner updateBannerStatus(Long id, boolean isActive) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Banner"));
        banner.setActive(isActive);
        return bannerRepository.save(banner);
    }
}