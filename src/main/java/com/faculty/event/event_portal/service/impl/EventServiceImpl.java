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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository; // Inject thêm cái này
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;


    public EventServiceImpl(EventRepository eventRepository,
                            UserRepository userRepository,
                            RegistrationRepository registrationRepository,
                            CategoryRepository categoryRepository,
                            NotificationService notificationService) { // Cập nhật constructor
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository; // Gán giá trị
        this.categoryRepository = categoryRepository;
        this.notificationService = notificationService;
    }

    // --- Hàm helper để chuyển Entity -> DTO ---
    private EventResponse convertToResponse(Event event, Boolean isRegistered) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setTieuDe(event.getTieuDe());
        response.setMoTaNgan(event.getMoTaNgan());
        response.setNoiDung(event.getNoiDung());
        response.setAnhThumbnail(event.getAnhThumbnail());
        response.setThoiGianBatDau(event.getThoiGianBatDau());
        response.setThoiGianKetThuc(event.getThoiGianKetThuc());
        response.setDiaDiem(event.getDiaDiem());
        response.setSoLuongGioiHan(event.getSoLuongGioiHan());
        long count = registrationRepository.countByEvent(event);
        response.setSoNguoiDaDangKy(count);
        response.setTrangThai(event.getTrangThai().name());
        response.setLuotXem(event.getLuotXem());
        // response.setTenNguoiDang(event.getNguoiDang().getHoTen());
        // 1. Map Người Đăng (Xử lý an toàn)
        try {
            if (event.getNguoiDang() != null) {
                // Lưu ý: Backend trả về String 'tenNguoiDang', không phải object
                response.setTenNguoiDang(event.getNguoiDang().getHoTen());
            } else {
                response.setTenNguoiDang("Admin");
            }
        } catch (Exception e) {
            response.setTenNguoiDang("Người dùng đã xóa");
        }

        // 2. Map Danh Mục (MỚI THÊM)
        if (event.getCategory() != null) {
            response.setTenDanhMuc(event.getCategory().getTenDanhMuc());

            // === THÊM DÒNG NÀY ===
            response.setCategoryId(event.getCategory().getId());
            // ====================
        } else {
            response.setTenDanhMuc("Sự kiện chung");
            response.setCategoryId(null);
        }
        response.setCreatedAt(event.getCreatedAt());
        response.setIsRegistered(isRegistered); // Gán giá trị mới
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
                .map(event -> convertToResponse(event, false)) // Mặc định là false
                .collect(Collectors.toList());
    }

    // Hàm lấy danh sách cho Admin (Đã loại bỏ DRAFT)
    @Override
    public List<EventResponse> getAllEventsForAdmin() {
        // Lấy tất cả sự kiện có trạng thái KHÔNG PHẢI LÀ DRAFT
        // Sắp xếp theo ngày tạo mới nhất (hoặc ngày bắt đầu tùy bạn)
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
            // Bỏ qua lỗi nếu không lấy được user (ví dụ: chưa đăng nhập)
            isRegistered = false;
        }

        return convertToResponse(event, isRegistered);
    }

    @Override
    public EventResponse createEvent(EventRequest request, String posterEmail) {
        // 1. Tìm user (người đăng)
        User poster = userRepository.findByEmail(posterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người đăng"));

        // 2. Chuyển DTO -> Entity
        Event event = new Event();
        event.setTieuDe(request.getTieuDe());
        event.setMoTaNgan(request.getMoTaNgan());
        event.setNoiDung(request.getNoiDung());
        event.setAnhThumbnail(request.getAnhThumbnail());
        event.setThoiGianBatDau(request.getThoiGianBatDau());
        event.setThoiGianKetThuc(request.getThoiGianKetThuc());
        event.setDiaDiem(request.getDiaDiem());
        event.setSoLuongGioiHan(request.getSoLuongGioiHan());
        event.setNguoiDang(poster); // Gán người tạo
        // === SỬA ĐOẠN SET TRẠNG THÁI ===
        if ("PENDING".equals(request.getTrangThai())) {
            event.setTrangThai(EventStatus.PENDING); // Gửi duyệt (Chờ duyệt)
        } else {
            event.setTrangThai(EventStatus.DRAFT); // Mặc định là Nháp
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Danh mục (Category) với ID: " + request.getCategoryId()));
            event.setCategory(category); // Gán đối tượng Category đã tìm được
        }

        // 3. Lưu vào CSDL
        Event savedEvent = eventRepository.save(event);

        // === THÊM ĐOẠN CODE THÔNG BÁO NÀY ===
        try {
            // 1. Tìm tất cả Admin
            List<User> admins = userRepository.findAllByRole(Role.ADMIN);

            // 2. Gửi thông báo cho từng Admin
            for (User admin : admins) {
                notificationService.createNotification(
                        admin,
                        "Sự kiện mới cần duyệt",
                        "Poster " + poster.getHoTen() + " vừa đăng sự kiện: " + savedEvent.getTieuDe(),
                        "WARNING" // Hoặc "INFO"
                );
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo cho Admin: " + e.getMessage());
        }

        return convertToResponse(savedEvent, false);
    }

    @Override
    public EventResponse updateEvent(Long id, EventRequest request, String posterEmail) {
        // 1. Tìm sự kiện
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        // 2. Tìm người dùng
        User poster = userRepository.findByEmail(posterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        // 3. Kiểm tra quyền: Chỉ chủ sở hữu mới được sửa
        if (!event.getNguoiDang().getId().equals(poster.getId())) {
            throw new AccessDeniedException("Bạn không có quyền sửa sự kiện này");
        }

        // 4. Cập nhật thông tin
        event.setTieuDe(request.getTieuDe());
        event.setMoTaNgan(request.getMoTaNgan());
        event.setNoiDung(request.getNoiDung());
        event.setAnhThumbnail(request.getAnhThumbnail());
        event.setThoiGianBatDau(request.getThoiGianBatDau());
        event.setThoiGianKetThuc(request.getThoiGianKetThuc());
        event.setDiaDiem(request.getDiaDiem());
        event.setSoLuongGioiHan(request.getSoLuongGioiHan());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Danh mục (Category) với ID: " + request.getCategoryId()));
            event.setCategory(category);
        } else {
            event.setCategory(null); // Cho phép gỡ bỏ category
        }

        if (request.getTrangThai() != null) {
            if ("PENDING".equals(request.getTrangThai())) {
                event.setTrangThai(EventStatus.PENDING); // Gửi duyệt

                // (Optional) Gửi thông báo cho Admin tại đây nếu muốn

            } else if ("DRAFT".equals(request.getTrangThai())) {
                event.setTrangThai(EventStatus.DRAFT); // Về nháp
            }
        }

        Event updatedEvent = eventRepository.save(event);
        return convertToResponse(updatedEvent, false);
    }

    @Override
    @Transactional
    public void deleteEvent(Long id, String userEmail) {
        // 1. Tìm sự kiện
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        // 2. Tìm người dùng
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        // 3. Kiểm tra quyền: Chủ sở hữu HOẶC ADMIN mới được xóa
        if (!event.getNguoiDang().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xóa sự kiện này");
        }

        // (Cần kiểm tra thêm: nếu đã có người đăng ký thì không cho xóa, mà chỉ nên "hủy")
        // Tạm thời chúng ta cho phép xóa
        eventRepository.delete(event);

//        // Đặt cờ xóa
//        event.setDeleted(true);
//
//        // QUAN TRỌNG: Lưu lại vào DB
//        eventRepository.save(event);
    }

    // 1. Admin TỪ CHỐI (Kèm lý do và thông báo)
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

        // === GỬI THÔNG BÁO CHO POSTER ===
        String notifyContent = "Sự kiện '" + event.getTieuDe() + "' đã bị từ chối.";
        if (reason != null && !reason.trim().isEmpty()) {
            notifyContent += " Lý do: " + reason;
        }

        notificationService.createNotification(
                event.getNguoiDang(), // Người nhận (Poster)
                "Sự kiện bị từ chối",  // Tiêu đề
                notifyContent,         // Nội dung kèm lý do
                "ERROR"                // Loại thông báo (Màu đỏ)
        );
    }

    // 2. Admin HỦY (Kèm lý do và thông báo)
    @Override
    public void cancelEvent(Long eventId, String reason) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        if (event.getTrangThai() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ hủy được sự kiện đã công khai");
        }

        // Đổi trạng thái
        event.setTrangThai(EventStatus.CANCELLED);
        eventRepository.save(event);

        // === GỬI THÔNG BÁO CHO POSTER ===
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

    // 1. Lấy danh sách thùng rác của Poster
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

    // 2. Khôi phục sự kiện (Dành cho Poster)
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

        // Thực hiện khôi phục
        eventRepository.restoreEvent(id);
    }

    // 3. Xóa vĩnh viễn (Dành cho Poster)
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

        eventRepository.deleteRegistrationsByEventId(id);
        eventRepository.permanentDelete(id);
    }

    // Import thêm: java.util.ArrayList, com.faculty.event.event_portal.dto.ParticipantResponse
    @Override
    public List<ParticipantResponse> getEventParticipants(Long eventId, String posterEmail) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));
        User poster = userRepository.findByEmail(posterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        // Kiểm tra: Chỉ chủ sự kiện hoặc Admin mới được xem
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
        // 1. Lấy dữ liệu (tái sử dụng hàm trên)
        List<ParticipantResponse> participants = getEventParticipants(eventId, posterEmail);

        // 2. Tạo file Excel trong bộ nhớ
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Danh sach tham gia");

        // 3. Tạo hàng tiêu đề (Header)
        String[] HEADERS = {"STT", "Họ tên", "MSSV", "Email", "Lớp", "Trạng thái vé", "Thời gian ĐK"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
        }

        // 4. Đổ dữ liệu
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

        // 5. Ghi vào output stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
}