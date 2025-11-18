package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.dto.*;
import com.faculty.event.event_portal.entity.*;
import com.faculty.event.event_portal.repository.*;
import com.faculty.event.event_portal.service.AdminService;
import jakarta.persistence.EntityNotFoundException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final CategoryRepository categoryRepository;
    private final BannerRepository bannerRepository;
    // Inject PasswordEncoder vào constructor của AdminServiceImpl
    private final PasswordEncoder passwordEncoder;
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
        response.setTrangThai(event.getTrangThai().name());
        response.setLuotXem(event.getLuotXem());
        response.setTenNguoiDang(event.getNguoiDang().getHoTen());
        response.setIsRegistered(isRegistered);

        return response;
    }

    // ... constructor
    public AdminServiceImpl(UserRepository userRepository,
                            EventRepository eventRepository,
                            RegistrationRepository registrationRepository,
                            CategoryRepository categoryRepository,
                            BannerRepository bannerRepository,
                            PasswordEncoder passwordEncoder) { // Thêm
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.categoryRepository = categoryRepository;
        this.bannerRepository = bannerRepository;
        this.passwordEncoder = passwordEncoder; // Thêm
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setHoTen(user.getHoTen());
        response.setEmail(user.getEmail());
        response.setMssv(user.getMssv());
        response.setRole(user.getRole().name());
        response.setSoDienThoai(user.getSoDienThoai());
        response.setKhoa(user.getKhoa());
        response.setLopHoc(user.getLopHoc());
        response.setNganhHoc(user.getNganhHoc());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    // Thêm hàm @Override mới
    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        User newUser = new User();
        newUser.setHoTen(request.getHoTen());
        newUser.setMssv(request.getMssv());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setSoDienThoai(request.getSoDienThoai());
        newUser.setNganhHoc(request.getNganhHoc());
        newUser.setLopHoc(request.getLopHoc());
        newUser.setKhoa(request.getKhoa());

        try {
            newUser.setRole(Role.valueOf(request.getRole().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Vai trò không hợp lệ: " + request.getRole());
        }

        User savedUser = userRepository.save(newUser);
        return convertToUserResponse(savedUser); // Dùng hàm convert bạn đã có
    }

    @Override
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user"));

        // Cập nhật các trường
        user.setHoTen(request.getHoTen());
        user.setMssv(request.getMssv());
        user.setSoDienThoai(request.getSoDienThoai());
        user.setNganhHoc(request.getNganhHoc());
        user.setLopHoc(request.getLopHoc());
        user.setKhoa(request.getKhoa());

        User updatedUser = userRepository.save(user);
        return convertToUserResponse(updatedUser);
    }

    @Override
    public UserResponse updateUserRole(Long userId, String newRoleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user"));

        try {
            Role newRole = Role.valueOf(newRoleName.toUpperCase());
            user.setRole(newRole);
            return convertToUserResponse(userRepository.save(user));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role không hợp lệ: " + newRoleName);
        }
    }

    @Override
    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        // 1. Kiểm tra mật khẩu cũ có khớp không
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác");
        }

        // 2. Mã hóa và cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public UserResponse getMyProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));
        return convertToUserResponse(user); // Dùng lại hàm convert đã có
    }

    @Override
    public UserResponse updateMyProfile(String userEmail, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        // Kiểm tra xem email mới (nếu có) đã bị ai khác dùng chưa
        if (request.getEmail() != null && !request.getEmail().equals(userEmail)) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email mới đã được sử dụng");
            }
            user.setEmail(request.getEmail());
        }

        // Cập nhật SĐT
        user.setSoDienThoai(request.getSoDienThoai());

        User updatedUser = userRepository.save(user);

        // Lưu ý: Nếu user đổi email, token cũ của họ vẫn hợp lệ
        // nhưng token mới sẽ cần được tạo ở lần đăng nhập sau.
        return convertToUserResponse(updatedUser);
    }

    @Override
    public void approveEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        event.setTrangThai(EventStatus.PUBLISHED);
        eventRepository.save(event);
    }

    @Override
    public List<EventResponse> getAllEventsForAdmin() {
        // Lấy tất cả sự kiện (chưa bị xóa mềm)
        List<Event> events = eventRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        // Cần kiểm tra đăng nhập của từng user để set isRegistered,
        // nhưng ở trang Admin, ta có thể mặc định là false
        return events.stream()
                .map(event -> convertToResponse(event, false)) // Dùng lại hàm convert
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getTopEventStats(int year, int month) {
        // Xác định khoảng thời gian
        LocalDateTime startDate;
        LocalDateTime endDate;

        if (month > 0 && month <= 12) {
            // Lọc theo tháng
            startDate = LocalDateTime.of(year, month, 1, 0, 0);
            endDate = startDate.plusMonths(1).minusNanos(1);
        } else {
            // Lọc theo cả năm
            startDate = LocalDateTime.of(year, 1, 1, 0, 0);
            endDate = startDate.plusYears(1).minusNanos(1);
        }

        List<Object[]> results = eventRepository.findTopEventsByRegistrationInDateRange(startDate, endDate);

        Map<String, Long> stats = new LinkedHashMap<>();
        for (Object[] result : results) {
            stats.put((String) result[0], (Long) result[1]);
        }
        return stats;
    }

    @Override
    public Map<Integer, Long> getMonthlyEventStats(int year) {
        List<Object[]> results = eventRepository.countEventsByMonth(year);
        // Khởi tạo map đủ 12 tháng với giá trị 0
        Map<Integer, Long> stats = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            stats.put(i, 0L);
        }
        // Điền dữ liệu thực tế vào
        for (Object[] row : results) {
            stats.put((Integer) row[0], (Long) row[1]);
        }
        return stats;
    }

    @Override
    public Map<String, Long> getTopCategoryStats() {
        List<Object[]> results = registrationRepository.findTopCategoriesByParticipation();
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Object[] row : results) {
            stats.put((String) row[0], (Long) row[1]);
        }
        return stats;
    }

    @Override
    public byte[] exportEventsToExcel() throws IOException {
        // Lấy tất cả sự kiện (chưa bị xóa)
        List<Event> events = eventRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Danh sách Sự kiện");

        // Tiêu đề
        String[] HEADERS = {"ID", "Tiêu đề", "Trạng thái", "Người tạo", "Ngày bắt đầu", "Địa điểm", "Số lượng ĐK"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            headerRow.createCell(i).setCellValue(HEADERS[i]);
        }

        // Đổ dữ liệu
        int rowNum = 1;
        for (Event event : events) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(event.getId());
            row.createCell(1).setCellValue(event.getTieuDe());
            row.createCell(2).setCellValue(event.getTrangThai().name());
            row.createCell(3).setCellValue(event.getNguoiDang().getHoTen());
            row.createCell(4).setCellValue(event.getThoiGianBatDau().toString()); // Cần format
            row.createCell(5).setCellValue(event.getDiaDiem());
            // Đếm số lượng đăng ký cho sự kiện này
            long registrationCount = registrationRepository.countByEvent(event);
            row.createCell(6).setCellValue(registrationCount);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    @Override
    public List<Category> getAllCategories() {
        // Chỉ lấy chưa xóa
        return categoryRepository.findAll();
    }

    @Override
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục"));
        category.setTenDanhMuc(categoryDetails.getTenDanhMuc());
        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục"));
        // Soft Delete thủ công
        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    // --- User Recycle Bin ---

    @Override
    public List<UserResponse> getDeletedUsers() {
        return userRepository.findSoftDeleted().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void restoreUser(Long userId) {
        User user = userRepository.findSoftDeletedById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user trong thùng rác"));

        user.setDeletedAt(null); // Gán lại là null để khôi phục
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void permanentDeleteUser(Long userId) {
        User user = userRepository.findSoftDeletedById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user trong thùng rác"));

        // Kiểm tra ràng buộc khóa ngoại (ví dụ)
        if (!eventRepository.findAllByNguoiDang(user).isEmpty()) {
            throw new IllegalStateException("Không thể xóa vĩnh viễn. User này đã tạo sự kiện. Hãy xóa sự kiện của họ trước.");
        }
        // (Thêm kiểm tra cho Registration nếu cần)

        userRepository.permanentDelete(userId); // Gọi hàm xóa vĩnh viễn
    }

    // --- Event Recycle Bin ---

    @Override
    public List<EventResponse> getDeletedEvents() {
        return eventRepository.findSoftDeleted().stream()
                .map(event -> convertToResponse(event, false)) // Mặc định false
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void restoreEvent(Long eventId) {
        Event event = eventRepository.findSoftDeletedById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện trong thùng rác"));
        event.setDeletedAt(null);
        eventRepository.save(event);
    }

    @Override
    @Transactional
    public void permanentDeleteEvent(Long eventId) {
        Event event = eventRepository.findSoftDeletedById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện trong thùng rác"));

        // Kiểm tra ràng buộc (ví dụ: đã có ai đăng ký chưa)
        if (registrationRepository.countByEvent(event) > 0) {
            throw new IllegalStateException("Không thể xóa vĩnh viễn. Sự kiện này đã có người đăng ký.");
        }

        eventRepository.permanentDelete(eventId);
    }

    // 3. Category Trash (Bổ sung)
    @Override
    public List<Category> getDeletedCategories() {
        return categoryRepository.findSoftDeleted();
    }

    @Override
    public Category restoreCategory(Long id) {
        categoryRepository.restoreCategory(id);
        return categoryRepository.findById(id).orElse(null);
    }

    @Override
    public void hardDeleteCategory(Long id) {
        categoryRepository.permanentDelete(id);
    }

    // 4. Banner Trash (Bổ sung)
    @Override
    public List<Banner> getDeletedBanners() {
        return bannerRepository.findSoftDeleted();
    }

    @Override
    public Banner restoreBanner(Long id) {
        bannerRepository.restoreBanner(id);
        return bannerRepository.findById(id).orElse(null);
    }

    @Override
    public void hardDeleteBanner(Long id) {
        bannerRepository.permanentDelete(id);
    }

    @Override
    public Map<String, Long> getDashboardStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalEvents", eventRepository.count());
        stats.put("totalRegistrations", registrationRepository.count());
        // Có thể thêm: số sự kiện đang chờ duyệt (DRAFT)...
        return stats;
    }

    @Override
    public Map<String, Long> getEventRegistrationStats() {
        List<Object[]> results = eventRepository.findTop5EventsByRegistration();
        // Dùng LinkedHashMap để giữ đúng thứ tự "Top 5"
        Map<String, Long> stats = new LinkedHashMap<>();

        for (Object[] result : results) {
            String eventName = (String) result[0];
            Long registrationCount = (Long) result[1];
            stats.put(eventName, registrationCount);
        }
        return stats;
    }
}