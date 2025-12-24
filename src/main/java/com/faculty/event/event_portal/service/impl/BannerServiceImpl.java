package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.entity.Banner;
import com.faculty.event.event_portal.repository.BannerRepository;
import com.faculty.event.event_portal.service.BannerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    @PersistenceContext
    private EntityManager entityManager;

    public BannerServiceImpl(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
        try { Files.createDirectories(this.fileStorageLocation); }
        catch (Exception ex) { throw new RuntimeException("Lỗi tạo thư mục uploads", ex); }
    }

    // Logic lưu file
    private String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.lastIndexOf(".") > 0) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return newFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Không thể lưu file", ex);
        }
    }

    // Logic xóa file
    private void deleteFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return;
        if (fileName.toLowerCase().startsWith("http")) return;

        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
            System.out.println("DEBUG: Đã xóa file: " + fileName);
        } catch (Exception ex) {
            System.err.println("WARN: Không xóa được file (vẫn tiếp tục xóa DB): " + ex.getMessage());
        }
    }

    @Override
    public List<Banner> getAllBanners() {
        return bannerRepository.findAllNotDeleted();
    }

    @Override
    public List<Banner> getActiveBanners() {
        return bannerRepository.findAllActive();
    }

    @Override
    public Banner createBanner(MultipartFile image, Boolean active) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn hình ảnh!");
        }
        Banner banner = new Banner();
        banner.setImageUrl(storeFile(image));
        banner.setActive(active != null ? active : true);
        return bannerRepository.save(banner);
    }

    @Override
    public Banner updateBanner(Long id, MultipartFile image, Boolean active) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Banner not found"));

        if (image != null && !image.isEmpty()) {
            deleteFile(banner.getImageUrl());
            banner.setImageUrl(storeFile(image));
        }
        if (active != null) banner.setActive(active);

        return bannerRepository.save(banner);
    }

    @Override
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Banner not found"));

        banner.setDeletedAt(LocalDateTime.now());
        banner.setActive(false);
        bannerRepository.save(banner);
    }

    @Override
    public Banner updateBannerStatus(Long id, boolean isActive) {
        Banner banner = bannerRepository.findById(id).orElseThrow();
        banner.setActive(isActive);
        return bannerRepository.save(banner);
    }

    @Override
    public List<Banner> getDeletedBanners() {
        return bannerRepository.findSoftDeleted();
    }

    @Override
    public void restoreBanner(Long id) {
        Banner banner = bannerRepository.findSoftDeletedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Banner not found in trash"));

        banner.setDeletedAt(null);
        banner.setActive(false);
        bannerRepository.save(banner);
    }

    @Override
    @Transactional
    public void hardDeleteBanner(Long id) {
        // Tìm banner trong thùng rác
        Banner banner = bannerRepository.findSoftDeletedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Banner not found in trash"));

        // Xóa file ảnh
        deleteFile(banner.getImageUrl());

        // Xóa vĩnh viễn trong DB bằng EntityManager
        entityManager.createNativeQuery("DELETE FROM banners WHERE id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }
}