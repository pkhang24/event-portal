package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.dto.EventRequest;
import com.faculty.event.event_portal.dto.EventResponse;
import com.faculty.event.event_portal.dto.ParticipantResponse;
import com.faculty.event.event_portal.entity.*;
import com.faculty.event.event_portal.repository.*;
import com.faculty.event.event_portal.service.EventService;
import com.faculty.event.event_portal.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;

    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    // Hàm hỗ trợ lưu file
    private String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        try {
            // Tạo thư mục nếu chưa có
            if (!Files.exists(fileStorageLocation)) {
                Files.createDirectories(fileStorageLocation);
            }

            // Tạo tên file ngẫu nhiên để tránh trùng (UUID)
            String originalFileName = file.getOriginalFilename();

            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String newFileName = UUID.randomUUID().toString() + fileExtension;

            Path targetLocation = fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return newFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Không thể lưu file " + file.getOriginalFilename(), ex);
        }
    }


    public EventServiceImpl(EventRepository eventRepository,
                            UserRepository userRepository,
                            RegistrationRepository registrationRepository,
                            CategoryRepository categoryRepository,
                            NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.categoryRepository = categoryRepository;
        this.notificationService = notificationService;
    }

    // --- Hàm helper để chuyển Entity sang DTO ---
    private EventResponse convertToResponse(Event event, Boolean isRegistered) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setTieuDe(event.getTieuDe());
        response.setMoTaNgan(event.getMoTaNgan());
        response.setNoiDung(event.getNoiDung());
        response.setAnhThumbnail(event.getAnhThumbnail());
        response.setAnhBia(event.getAnhBia());
        response.setThoiGianBatDau(event.getThoiGianBatDau());
        response.setThoiGianKetThuc(event.getThoiGianKetThuc());
        response.setDiaDiem(event.getDiaDiem());
        response.setSoLuongGioiHan(event.getSoLuongGioiHan());
        long count = registrationRepository.countByEvent(event);
        response.setSoNguoiDaDangKy(count);
        response.setTrangThai(event.getTrangThai().name());
        response.setLuotXem(event.getLuotXem());
        // response.setTenNguoiDang(event.getNguoiDang().getHoTen());
        try {
            if (event.getNguoiDang() != null) {
                response.setTenNguoiDang(event.getNguoiDang().getHoTen());
            } else {
                response.setTenNguoiDang("Admin");
            }
        } catch (Exception e) {
            response.setTenNguoiDang("Người dùng đã xóa");
        }

        if (event.getCategory() != null) {
            try {
                response.setTenDanhMuc(event.getCategory().getTenDanhMuc());
                response.setCategoryId(event.getCategory().getId());
            } catch (EntityNotFoundException | NullPointerException e) {
                response.setTenDanhMuc("Danh mục đã xóa");
                response.setCategoryId(null);
            }
        } else {
            response.setTenDanhMuc("Chưa phân loại");
            response.setCategoryId(null);
        }
        response.setCreatedAt(event.getCreatedAt());
        response.setIsRegistered(isRegistered);
//        response.setDeleted(event.isDeleted());
        return response;
    }

    @Override
    public List<EventResponse> getAllPublishedEvents(String search, String status, Long categoryId) {
        Specification<Event> spec = EventSpecification.findByCriteria(search, status, categoryId);

        // Sắp xếp theo ngày bắt đầu tăng dần
        Sort sort = Sort.by(Sort.Direction.ASC, "thoiGianBatDau");

        return eventRepository.findAll(spec, sort) // Dùng findAll có Specification
                .stream()
                .map(event -> convertToResponse(event, false))
                .collect(Collectors.toList());
    }

    // Hàm lấy danh sách cho Admin (Đã loại bỏ DRAFT)
    @Override
    public List<EventResponse> getAllEventsForAdmin() {
        // Lấy tất cả sự kiện có trạng thái KHÔNG PHẢI LÀ DRAFT
        List<Event> events = eventRepository.findAllByTrangThaiNot(
                EventStatus.DRAFT,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return events.stream()
                .map(event -> convertToResponse(event, false))
                .collect(Collectors.toList());
    }

    // Sửa hàm getEventById
    @Override
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        // Tăng view
        event.setLuotXem(event.getLuotXem() + 1);
        eventRepository.save(event);

        // --- KIỂM TRA ĐĂNG KÝ ---
        Boolean isRegistered = false;
        try {
            // Lấy email người dùng hiện tại từ SecurityContext
            String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            if (currentEmail != null && !currentEmail.equals("anonymousUser")) {
                User currentUser = userRepository.findByEmail(currentEmail).orElse(null);
                if (currentUser != null) {
                    // Kiểm tra xem user này đã đăng ký event này chưa
                    isRegistered = registrationRepository.existsByUserAndEvent(currentUser, event);
                }
            }
        } catch (Exception e) {
            // Bỏ qua lỗi nếu không lấy được user
            isRegistered = false;
        }

        return convertToResponse(event, isRegistered);
    }

    @Override
    public EventResponse createEvent(EventRequest request, MultipartFile image, MultipartFile coverImage, String posterEmail) {
        // Tìm user (người đăng)
        User poster = userRepository.findByEmail(posterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người đăng"));

        // Chuyển DTO ssang Entity
        Event event = new Event();
        event.setTieuDe(request.getTieuDe());
        event.setMoTaNgan(request.getMoTaNgan());
        event.setNoiDung(request.getNoiDung());
//        event.setAnhThumbnail(request.getAnhThumbnail());
        if (image != null && !image.isEmpty()) {
            String fileName = storeFile(image);
            event.setAnhThumbnail(fileName);
        } else {
            event.setAnhThumbnail(null);
        }
        if (coverImage != null && !coverImage.isEmpty()) {
            String coverName = storeFile(coverImage);
            event.setAnhBia(coverName);
        } else {
            event.setAnhBia(null);
        }
        event.setThoiGianBatDau(request.getThoiGianBatDau());
        event.setThoiGianKetThuc(request.getThoiGianKetThuc());
        event.setDiaDiem(request.getDiaDiem());
        event.setSoLuongGioiHan(request.getSoLuongGioiHan());
        event.setNguoiDang(poster);

        if ("PENDING".equals(request.getTrangThai())) {
            event.setTrangThai(EventStatus.PENDING);
        } else {
            event.setTrangThai(EventStatus.DRAFT);
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Danh mục (Category) với ID: " + request.getCategoryId()));
            event.setCategory(category);
        }

        Event savedEvent = eventRepository.save(event);

        try {
            List<User> admins = userRepository.findAllByRole(Role.ADMIN);

            for (User admin : admins) {
                notificationService.createNotification(
                        admin,
                        "Sự kiện mới cần duyệt",
                        "Poster " + poster.getHoTen() + " vừa đăng sự kiện: " + savedEvent.getTieuDe(),
                        "WARNING"
                );
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo cho Admin: " + e.getMessage());
        }

        return convertToResponse(savedEvent, false);
    }

    @Override
    public EventResponse updateEvent(Long id, EventRequest request, MultipartFile image, MultipartFile coverImage, String posterEmail) {
        // Tìm sự kiện
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        // Tìm người dùng
        User poster = userRepository.findByEmail(posterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        // Kiểm tra quyền: Chỉ chủ sở hữu mới được sửa
        if (!event.getNguoiDang().getId().equals(poster.getId())) {
            throw new AccessDeniedException("Bạn không có quyền sửa sự kiện này");
        }

        // Cập nhật thông tin
        event.setTieuDe(request.getTieuDe());
        event.setMoTaNgan(request.getMoTaNgan());
        event.setNoiDung(request.getNoiDung());
//        event.setAnhThumbnail(request.getAnhThumbnail());
        if (image != null && !image.isEmpty()) {
            deleteFile(event.getAnhThumbnail());
            String fileName = storeFile(image);
            event.setAnhThumbnail(fileName);
        }
        if (coverImage != null && !coverImage.isEmpty()) {
            deleteFile(event.getAnhBia());
            String coverName = storeFile(coverImage);
            event.setAnhBia(coverName);
        }
        event.setThoiGianBatDau(request.getThoiGianBatDau());
        event.setThoiGianKetThuc(request.getThoiGianKetThuc());
        event.setDiaDiem(request.getDiaDiem());
        event.setSoLuongGioiHan(request.getSoLuongGioiHan());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Danh mục (Category) với ID: " + request.getCategoryId()));
            event.setCategory(category);
        } else {
            event.setCategory(null);
        }

        if (request.getTrangThai() != null) {
            if ("PENDING".equals(request.getTrangThai())) {
                event.setTrangThai(EventStatus.PENDING);

            } else if ("DRAFT".equals(request.getTrangThai())) {
                event.setTrangThai(EventStatus.DRAFT);
            }
        }

        Event updatedEvent = eventRepository.save(event);
        return convertToResponse(updatedEvent, false);
    }

    @Override
    @Transactional
    public void deleteEvent(Long id, String userEmail) {
        // Tìm sự kiện
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        // Tìm người dùng
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        // Kiểm tra quyền: Chủ sở hữu HOẶC ADMIN mới được xóa
        if (!event.getNguoiDang().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xóa sự kiện này");
        }

        // THỰC HIỆN XÓA MỀM (Soft Delete)
        event.setDeletedAt(LocalDateTime.now());

        eventRepository.save(event);
    }

    // Admin TỪ CHỐI
    @Override
    public void rejectEvent(Long eventId, String reason) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        if (event.getTrangThai() != EventStatus.PENDING) {
            throw new IllegalStateException("Chỉ từ chối được sự kiện đang chờ duyệt");
        }

        // Đổi trạng thái
        event.setTrangThai(EventStatus.DRAFT);
        eventRepository.save(event);

        // GỬI THÔNG BÁO CHO POSTER
        String notifyContent = "Sự kiện '" + event.getTieuDe() + "' đã bị từ chối.";
        if (reason != null && !reason.trim().isEmpty()) {
            notifyContent += " Lý do: " + reason;
        }

        notificationService.createNotification(
                event.getNguoiDang(),
                "Sự kiện bị từ chối",
                notifyContent,
                "ERROR"
        );
    }

    // Admin HỦY
    @Override
    public void cancelEvent(Long eventId, String reason) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        // Chỉ cho phép hủy nếu sự kiện đã Public
        if (event.getTrangThai() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ hủy được sự kiện đã công khai");
        }

        // Đổi trạng thái sang CANCELLED
        event.setTrangThai(EventStatus.CANCELLED);
        eventRepository.save(event);

        // GỬI THÔNG BÁO CHO POSTER
        String notifyContent = "Sự kiện '" + event.getTieuDe() + "' đã bị Admin hủy bỏ.";
        if (reason != null && !reason.trim().isEmpty()) {
            notifyContent += " Lý do: " + reason;
        }

        notificationService.createNotification(
                event.getNguoiDang(),
                "Sự kiện bị hủy",
                notifyContent,
                "ERROR"
        );
    }

    @Override
    public List<EventResponse> getMyEvents(String posterEmail) {
        User poster = userRepository.findByEmail(posterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        return eventRepository.findAllByNguoiDang(poster)
                .stream()
                // Khi poster xem sự kiện của mình, không cần check "isRegistered"
                .map(event -> convertToResponse(event, false))
                .collect(Collectors.toList());
    }

    // Lấy danh sách thùng rác của Poster
    @Override
    public List<EventResponse> getMyDeletedEvents(String posterEmail) {
        User poster = userRepository.findByEmail(posterEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Gọi Native Query để tìm các bài đã xóa của user này
        List<Event> deletedEvents = eventRepository.findSoftDeletedByUserId(poster.getId());

        return deletedEvents.stream()
                .map(event -> convertToResponse(event, false))
                .collect(Collectors.toList());
    }

    // Khôi phục sự kiện (Dành cho Poster)
    @Override
    @Transactional
    public void restoreEvent(Long id, String userEmail) {
        // Tìm sự kiện trong thùng rác
        Event event = eventRepository.findDeletedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện trong thùng rác"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Check quyền: Phải là chủ sở hữu hoặc Admin
        if (!event.getNguoiDang().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền khôi phục sự kiện này");
        }

        eventRepository.restoreEvent(id);
    }

    // Hàm xóa file khỏi ổ cứng
    private void deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) return;

        if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
            return;
        }

        try {
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
            System.out.println("Đã xóa file: " + fileName);
        } catch (Exception ex) {
            System.err.println("Không thể xóa file: " + fileName + ". Lỗi: " + ex.getMessage());
        }
    }

    // Xóa vĩnh viễn (Dành cho Poster)
    @Override
    @Transactional
    public void permanentDelete(Long id, String userEmail) {
        Event event = eventRepository.findDeletedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!event.getNguoiDang().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xóa vĩnh viễn sự kiện này");
        }

        deleteFile(event.getAnhThumbnail());
        deleteFile(event.getAnhBia());

//        eventRepository.delete(event);
        eventRepository.deleteRegistrationsByEventId(id);
        eventRepository.permanentDelete(id);
    }

    @Override
    public List<ParticipantResponse> getEventParticipants(Long eventId, String posterEmail) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));
        User poster = userRepository.findByEmail(posterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        // Chỉ chủ sự kiện hoặc Admin mới được xem
        if (!event.getNguoiDang().getId().equals(poster.getId()) && poster.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xem danh sách này");
        }

        // Lấy tất cả vé của sự kiện này
        List<Registration> registrations = registrationRepository.findAllByEvent(event);

        List<ParticipantResponse> participants = new ArrayList<>();
        for (Registration reg : registrations) {
            User student = reg.getUser();
            ParticipantResponse dto = new ParticipantResponse();
            dto.setUserId(student.getId());
            dto.setHoTen(student.getHoTen());
            dto.setMssv(student.getMssv());
            dto.setEmail(student.getEmail());
            dto.setLopHoc(student.getLopHoc());
            dto.setTrangThaiVe(reg.getTrangThai().name());
            dto.setThoiGianDangKy(reg.getCreatedAt());
            participants.add(dto);
        }
        return participants;
    }

    @Override
    public byte[] exportEventParticipantsToExcel(Long eventId, String posterEmail) throws IOException {
        // Lấy dữ liệu (tái sử dụng hàm trên)
        List<ParticipantResponse> participants = getEventParticipants(eventId, posterEmail);

        // Tạo file Excel trong bộ nhớ
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Danh sach tham gia");

        // Tạo hàng tiêu đề (Header)
        String[] HEADERS = {"STT", "Họ tên", "MSSV", "Email", "Lớp", "Trạng thái vé", "Thời gian ĐK"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
        }

        // Đổ dữ liệu
        int rowNum = 1;
        for (ParticipantResponse p : participants) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(p.getHoTen());
            row.createCell(2).setCellValue(p.getMssv());
            row.createCell(3).setCellValue(p.getEmail());
            row.createCell(4).setCellValue(p.getLopHoc());
            row.createCell(5).setCellValue(p.getTrangThaiVe().equals("ATTENDED") ? "Đã điểm danh" : "Chưa điểm danh");
            row.createCell(6).setCellValue(p.getThoiGianDangKy().toString()); // Cần format đẹp hơn
        }

        // Ghi vào output stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
}