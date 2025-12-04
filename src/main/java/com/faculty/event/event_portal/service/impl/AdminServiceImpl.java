package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.dto.*;
import com.faculty.event.event_portal.entity.*;
import com.faculty.event.event_portal.repository.*;
import com.faculty.event.event_portal.service.AdminService;
import com.faculty.event.event_portal.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final CategoryRepository categoryRepository;
    private final BannerRepository bannerRepository;
    private final NotificationService notificationService;

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
//        response.setTenNguoiDang(event.getNguoiDang().getHoTen());
        try {
            if (event.getNguoiDang() != null) {
                response.setTenNguoiDang(event.getNguoiDang().getHoTen());
            } else {
                response.setTenNguoiDang("Người dùng bị đã xóa");
            }
        } catch (EntityNotFoundException ex) {
            // Bắt lỗi khi Hibernate cố load user đã bị xóa mềm
            response.setTenNguoiDang("Người dùng bị đã xóa");
        }
        response.setIsRegistered(isRegistered);

        return response;
    }

    // ... constructor
    public AdminServiceImpl(UserRepository userRepository,
                            EventRepository eventRepository,
                            RegistrationRepository registrationRepository,
                            CategoryRepository categoryRepository,
                            BannerRepository bannerRepository,
                            PasswordEncoder passwordEncoder,
                            NotificationService notificationService) { // Thêm
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.categoryRepository = categoryRepository;
        this.bannerRepository = bannerRepository;
        this.notificationService = notificationService;
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
        response.setLocked(user.isLocked());
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
        // 1. Kiểm tra trùng Email (Đã có)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email này đã tồn tại trong hệ thống!");
        }

        // 2. === THÊM: Kiểm tra trùng MSSV ===
        // (Chỉ kiểm tra nếu MSSV không rỗng)
        if (request.getMssv() != null && !request.getMssv().isEmpty()) {
            if (userRepository.existsByMssv(request.getMssv())) {
                throw new IllegalArgumentException("Mã số sinh viên này đã tồn tại!");
            }
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
        // user.setMssv(request.getMssv());
        user.setSoDienThoai(request.getSoDienThoai());
        user.setNganhHoc(request.getNganhHoc());
        user.setLopHoc(request.getLopHoc());
        user.setKhoa(request.getKhoa());

        // 2. Kiểm tra MSSV (Quan trọng)
        if (request.getMssv() != null && !request.getMssv().trim().isEmpty()) {
            String newMssv = request.getMssv().trim();
            String oldMssv = user.getMssv();

            // Logic: Nếu MSSV CÓ THAY ĐỔI và MSSV MỚI đã tồn tại trong hệ thống
            if (!newMssv.equalsIgnoreCase(oldMssv) && userRepository.existsByMssv(newMssv)) {
                throw new IllegalArgumentException("Mã số sinh viên '" + newMssv + "' đã được sử dụng bởi tài khoản khác!");
            }
            user.setMssv(newMssv);
        }

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
    public void toggleUserLock(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user"));

        // Đảo ngược trạng thái (Đang khóa -> Mở, Đang mở -> Khóa)
        user.setLocked(!user.isLocked());
        userRepository.save(user);
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

        // === 3. THÊM ĐOẠN NÀY: Báo cho Poster ===
        try {
            notificationService.createNotification(
                    event.getNguoiDang(), // Người nhận là Poster
                    "Sự kiện đã được duyệt",
                    "Sự kiện '" + event.getTieuDe() + "' của bạn đã được Admin phê duyệt.",
                    "SUCCESS"
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo: " + e.getMessage());
        }
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
    public Map<String, Long> getTopCategoryStats(int year, int month) {
        // 1. Tính toán khoảng thời gian (Giống hệt hàm getTopEventStats)
        LocalDateTime startDate;
        LocalDateTime endDate;

        if (month > 0 && month <= 12) {
            startDate = LocalDateTime.of(year, month, 1, 0, 0);
            endDate = startDate.plusMonths(1).minusNanos(1);
        } else {
            startDate = LocalDateTime.of(year, 1, 1, 0, 0);
            endDate = startDate.plusYears(1).minusNanos(1);
        }

        // 2. Gọi Repository mới có lọc ngày
        List<Object[]> results = registrationRepository.findCategoryStatsInDateRange(startDate, endDate);

        Map<String, Long> stats = new LinkedHashMap<>();
        for (Object[] row : results) {
            stats.put((String) row[0], (Long) row[1]);
        }
        return stats;
    }

    @Override
    public byte[] exportDashboardReport(int year) throws IOException {
        // 1. Lấy dữ liệu thống kê
        Map<String, Long> topEvents = getTopEventStats(year, 0); // 0 = cả năm
        Map<Integer, Long> monthlyStats = getMonthlyEventStats(year);
        Map<String, Long> categoryStats = getTopCategoryStats(year, 0);

        // 2. Khởi tạo Workbook & Sheet
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Báo cáo Thống kê " + year);

        // --- TẠO STYLE (Định dạng đẹp) ---

        // Style: Tiêu đề lớn (Bold, Center, Font to)
        CellStyle titleStyle = workbook.createCellStyle();
        XSSFFont titleFont = (XSSFFont) workbook.createFont();
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        // Style: Header bảng (Bold, Nền xám, Có viền)
        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont headerFont = (XSSFFont) workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Style: Data (Có viền)
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        int rowNum = 0;

        // === PHẦN 1: TIÊU ĐỀ CHUNG ===
        Row mainTitleRow = sheet.createRow(rowNum++);
        Cell mainTitleCell = mainTitleRow.createCell(0);
        mainTitleCell.setCellValue("BÁO CÁO HOẠT ĐỘNG NĂM " + year);
        mainTitleCell.setCellStyle(titleStyle);
        // Merge cell (Gộp ô A1 đến D1)
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        rowNum++; // Cách 1 dòng

        // === PHẦN 2: TOP SỰ KIỆN (Top 5) ===
        rowNum = createSectionHeader(sheet, rowNum, "I. TOP SỰ KIỆN CÓ LƯỢT ĐĂNG KÝ CAO NHẤT", titleStyle);
        // Header Table
        Row headerRow1 = sheet.createRow(rowNum++);
        createCell(headerRow1, 0, "Tên sự kiện", headerStyle);
        createCell(headerRow1, 1, "Số lượng đăng ký", headerStyle);

        // Data Table
        for (Map.Entry<String, Long> entry : topEvents.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, entry.getKey(), dataStyle);
            createCell(row, 1, entry.getValue().toString(), dataStyle);
        }
        rowNum++; // Cách dòng

        // === PHẦN 3: THỐNG KÊ THEO THÁNG ===
        rowNum = createSectionHeader(sheet, rowNum, "II. SỐ LƯỢNG SỰ KIỆN THEO THÁNG", titleStyle);
        Row headerRow2 = sheet.createRow(rowNum++);
        createCell(headerRow2, 0, "Tháng", headerStyle);
        createCell(headerRow2, 1, "Số lượng sự kiện", headerStyle);

        for (Map.Entry<Integer, Long> entry : monthlyStats.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, "Tháng " + entry.getKey(), dataStyle);
            createCell(row, 1, entry.getValue().toString(), dataStyle);
        }
        rowNum++;

        // === PHẦN 4: TỈ LỆ THEO CHỦ ĐỀ ===
        rowNum = createSectionHeader(sheet, rowNum, "III. THỐNG KÊ THEO CHỦ ĐỀ", titleStyle);
        Row headerRow3 = sheet.createRow(rowNum++);
        createCell(headerRow3, 0, "Chủ đề (Danh mục)", headerStyle);
        createCell(headerRow3, 1, "Số lượng", headerStyle); // Hoặc "Lượt tham gia" tùy logic bạn chọn

        for (Map.Entry<String, Long> entry : categoryStats.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, entry.getKey(), dataStyle);
            createCell(row, 1, entry.getValue().toString(), dataStyle);
        }

        // Auto-size cột cho đẹp
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(0, 30 * 256); // Set độ rộng tối thiểu cho cột A

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    // Hàm phụ để tạo Cell nhanh
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // Hàm phụ tạo tiêu đề Section
    private int createSectionHeader(Sheet sheet, int rowNum, String title, CellStyle style) {
        Row row = sheet.createRow(rowNum++);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        // Tạo style riêng cho sub-header nếu muốn (nhỏ hơn title chính nhưng đậm)
        // Ở đây dùng tạm style title
        return rowNum;
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAllByDeletedAtIsNull();

        return categories.stream().map(cat -> {
            CategoryResponse dto = new CategoryResponse();
            dto.setId(cat.getId());
            dto.setTenDanhMuc(cat.getTenDanhMuc());

            // Gọi hàm đếm từ Repository
            long count = categoryRepository.countEventsByCategory(cat);
            dto.setSoLuongSuKien(count);

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Category createCategory(Category category) {
        // Kiểm tra trùng tên (Bỏ qua hoa thường & khoảng trắng)
        String tenClean = category.getTenDanhMuc().trim();

        if (categoryRepository.existsByTenDanhMucIgnoreCase(tenClean)) {
            throw new IllegalArgumentException("Tên danh mục này đã tồn tại!");
        }

        category.setTenDanhMuc(tenClean); // Lưu tên đã chuẩn hóa (đã trim)
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục"));

        String oldName = category.getTenDanhMuc();
        String newName = categoryDetails.getTenDanhMuc().trim();

        // Logic: Nếu tên CÓ THAY ĐỔI và tên MỚI đã tồn tại
        if (!newName.equalsIgnoreCase(oldName) && categoryRepository.existsByTenDanhMucIgnoreCase(newName)) {
            throw new IllegalArgumentException("Tên danh mục '" + newName + "' đã tồn tại!");
        }

        category.setTenDanhMuc(newName);
        // (Cập nhật các trường khác nếu có, ví dụ mô tả)

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

    @Override
    public List<DashboardActivity> getRecentActivities() {
        List<DashboardActivity> activities = new ArrayList<>();

        // 1. Lấy 5 sự kiện mới nhất
        List<Event> recentEvents = eventRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        for (Event e : recentEvents) {
            try {
                // Cố gắng lấy tên người đăng
                // Nếu User đã bị xóa mềm, dòng e.getNguoiDang() có thể gây lỗi EntityNotFound
                String tenNguoiDang = e.getNguoiDang() != null ? e.getNguoiDang().getHoTen() : "Người dùng đã xóa";

                activities.add(new DashboardActivity(
                        "Sự kiện \"" + e.getTieuDe() + "\" đã được tạo bởi " + tenNguoiDang,
                        e.getCreatedAt(),
                        "create_event"
                ));
            } catch (EntityNotFoundException ex) {
                // Nếu User bị xóa mềm và Hibernate không tìm thấy -> Bỏ qua hoặc hiện placeholder
                activities.add(new DashboardActivity(
                        "Sự kiện \"" + e.getTieuDe() + "\" (Người đăng đã bị xóa)",
                        e.getCreatedAt(),
                        "create_event"
                ));
            }
        }

        // 2. Lấy 5 lượt đăng ký mới nhất
        List<Registration> recentRegs = registrationRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        for (Registration r : recentRegs) {
            try {
                // Tương tự, kiểm tra User và Event của vé
                String tenSinhVien = r.getUser() != null ? r.getUser().getHoTen() : "SV đã xóa";
                String tenSuKien = r.getEvent() != null ? r.getEvent().getTieuDe() : "Sự kiện đã xóa";

                activities.add(new DashboardActivity(
                        tenSinhVien + " đã đăng ký tham gia \"" + tenSuKien + "\".",
                        r.getCreatedAt(),
                        "register"
                ));
            } catch (EntityNotFoundException ex) {
                // Bỏ qua nếu dữ liệu liên kết bị lỗi
            }
        }

        // 3. Lấy 5 user mới nhất
        List<User> recentUsers = userRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        for (User u : recentUsers) {
            activities.add(new DashboardActivity(
                    "Thành viên mới: " + u.getHoTen() + " đã gia nhập.",
                    u.getCreatedAt(),
                    "new_user"
            ));
        }

        return activities.stream()
                .sorted(Comparator.comparing(DashboardActivity::getTime).reversed())
                .limit(8)
                .collect(Collectors.toList());
    }
}