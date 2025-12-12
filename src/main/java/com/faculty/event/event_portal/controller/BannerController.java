package com.faculty.event.event_portal.controller;

import com.faculty.event.event_portal.entity.Banner;
import com.faculty.event.event_portal.service.BannerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // Import quan trọng

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    // API Public: Lấy banner active
    @GetMapping("/active")
    public ResponseEntity<List<Banner>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }

    /// Lấy danh sách (Admin)
    @GetMapping
    public ResponseEntity<List<Banner>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    // Tạo mới (Giống hệt Event: nhận file 'image')
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Banner> createBanner(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        return ResponseEntity.ok(bannerService.createBanner(image, active));
    }

    // Cập nhật
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Banner> updateBanner(
            @PathVariable Long id,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        return ResponseEntity.ok(bannerService.updateBanner(id, image, active));
    }

    // Các API xóa giữ nguyên...
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/banners/trash
    @GetMapping("/trash")
    public ResponseEntity<List<Banner>> getDeletedBanners() {
        return ResponseEntity.ok(bannerService.getDeletedBanners());
    }

    // PUT /api/banners/trash/restore/{id}
    @PutMapping("/trash/restore/{id}")
    public ResponseEntity<Void> restoreBanner(@PathVariable Long id) {
        bannerService.restoreBanner(id);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/banners/trash/hard-delete/{id}
    @DeleteMapping("/trash/hard-delete/{id}")
    public ResponseEntity<Void> hardDeleteBanner(@PathVariable Long id) {
        bannerService.hardDeleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}