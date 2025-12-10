package com.faculty.event.event_portal.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    // Thư mục lưu ảnh (sẽ nằm ngay trong thư mục project của bạn)
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    public FileStorageService() {
        try {
            // Tự động tạo thư mục uploads nếu chưa có
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Không thể tạo thư mục upload.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // Tạo tên file ngẫu nhiên để không bị trùng (dùng UUID)
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        try {
            // Copy file vào thư mục đích
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Không thể lưu file " + fileName, ex);
        }
    }
}