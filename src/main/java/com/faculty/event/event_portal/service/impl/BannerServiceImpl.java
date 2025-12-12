package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.entity.Banner;
import com.faculty.event.event_portal.repository.BannerRepository;
import com.faculty.event.event_portal.service.BannerService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
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

    // Logic xóa file khỏi ổ cứng
    private void deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) return;
        // Nếu là link ảnh online (http...) thì không xóa
        if (fileName.startsWith("http")) return;

        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
            System.out.println("Đã xóa file rác: " + fileName);
        } catch (IOException ex) {
            System.err.println("Lỗi xóa file: " + fileName);
        }
    }

    @Override
    public List<Banner> getAllBanners() {
        return bannerRepository.findAll();
    }

    @Override
    public List<Banner> getActiveBanners() {
        return bannerRepository.findAllByIsActiveTrue();
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
            // === SỬA QUAN TRỌNG: Xóa ảnh cũ trước khi lưu ảnh mới ===
            deleteFile(banner.getImageUrl());

            // Lưu ảnh mới
            banner.setImageUrl(storeFile(image));
        }
        if (active != null) banner.setActive(active);

        return bannerRepository.save(banner);
    }

    @Override
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Banner not found"));

        // Soft Delete: Gán thời gian xóa và ẩn đi
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
        // LƯU Ý: Vì Entity dùng @Where(clause="deleted_at IS NULL"), findById thường sẽ KHÔNG tìm thấy file đã xóa.
        // Bạn cần dùng hàm Query Native trong Repository để tìm.
        // Ví dụ: bannerRepository.findDeletedById(id)
        // Tạm thời tôi dùng findById, nhưng nếu lỗi "Not Found", bạn cần thêm hàm query native vào Repository.

        Banner banner = bannerRepository.findDeletedById(id) // <--- Cần kiểm tra kỹ chỗ này
                .orElseThrow(() -> new EntityNotFoundException("Banner not found (in trash)"));

        banner.setDeletedAt(null);
        banner.setActive(false);
        bannerRepository.save(banner);
    }

    @Override
    public void hardDeleteBanner(Long id) {
        Banner banner = bannerRepository.findDeletedById(id) // <--- Cần kiểm tra kỹ chỗ này
                .orElseThrow(() -> new EntityNotFoundException("Banner not found (in trash)"));

        // 1. Xóa ảnh trong ổ cứng để dọn rác
        deleteFile(banner.getImageUrl());

        // 2. Xóa vĩnh viễn trong DB
        bannerRepository.delete(banner);
    }
}