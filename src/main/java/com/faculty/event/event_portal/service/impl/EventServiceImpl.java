package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.dto.EventRequest;
import com.faculty.event.event_portal.dto.EventResponse;
import com.faculty.event.event_portal.dto.ParticipantResponse;
import com.faculty.event.event_portal.entity.*;
import com.faculty.event.event_portal.repository.*;
import com.faculty.event.event_portal.service.EventService;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository; // Inject thêm cái này
    private final CategoryRepository categoryRepository;


    public EventServiceImpl(EventRepository eventRepository,
                            UserRepository userRepository,
                            RegistrationRepository registrationRepository,
                            CategoryRepository categoryRepository) { // Cập nhật constructor
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository; // Gán giá trị
        this.categoryRepository = categoryRepository;
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
//        response.setTenNguoiDang(event.getNguoiDang().getHoTen());
        try {
            if (event.getNguoiDang() != null) {
                response.setTenNguoiDang(event.getNguoiDang().getHoTen());
            } else {
                response.setTenNguoiDang("Người dùng đã bị xóa");
            }
        } catch (EntityNotFoundException ex) {
            // Bắt lỗi khi Hibernate cố proxy tới user đã xóa mềm
            response.setTenNguoiDang("Người dùng đã bị xóa");
        }
        response.setCreatedAt(event.getCreatedAt());
        response.setIsRegistered(isRegistered); // Gán giá trị mới
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
        event.setTrangThai(EventStatus.DRAFT); // Mặc định là bản nháp, ADMIN sẽ duyệt sau

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Danh mục (Category) với ID: " + request.getCategoryId()));
            event.setCategory(category); // Gán đối tượng Category đã tìm được
        }

        // 3. Lưu vào CSDL
        Event savedEvent = eventRepository.save(event);

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

        Event updatedEvent = eventRepository.save(event);
        return convertToResponse(updatedEvent, false);
    }

    @Override
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