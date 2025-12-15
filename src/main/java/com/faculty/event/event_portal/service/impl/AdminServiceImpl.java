package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.dto.*;
import com.faculty.event.event_portal.entity.*;
import com.faculty.event.event_portal.repository.*;
import com.faculty.event.event_portal.service.AdminService;
import com.faculty.event.event_portal.service.NotificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
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

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final CategoryRepository categoryRepository;
    private final BannerRepository bannerRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public AdminServiceImpl(UserRepository userRepository,
                            EventRepository eventRepository,
                            RegistrationRepository registrationRepository,
                            CategoryRepository categoryRepository,
                            BannerRepository bannerRepository,
                            PasswordEncoder passwordEncoder,
                            NotificationService notificationService,
                            NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.categoryRepository = categoryRepository;
        this.bannerRepository = bannerRepository;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ... (Các hàm convert và helper giữ nguyên)
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

        try {
            User poster = event.getNguoiDang();
            if (poster != null) {
                // Nếu User bị khóa (Soft delete) -> Hiển thị là Admin
                if (poster.getDeletedAt() != null || poster.isLocked()) {
                    response.setTenNguoiDang("Admin");
                } else {
                    response.setTenNguoiDang(poster.getHoTen());
                }
            } else {
                response.setTenNguoiDang("Admin");
            }
        } catch (EntityNotFoundException ex) {
            response.setTenNguoiDang("Admin");
        }

        response.setIsRegistered(isRegistered);
        return response;
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

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email này đã tồn tại trong hệ thống!");
        }

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
        return convertToUserResponse(savedUser);
    }

    @Override
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user"));

        user.setHoTen(request.getHoTen());
        user.setSoDienThoai(request.getSoDienThoai());
        user.setNganhHoc(request.getNganhHoc());
        user.setLopHoc(request.getLopHoc());
        user.setKhoa(request.getKhoa());

        if (request.getMssv() != null && !request.getMssv().trim().isEmpty()) {
            String newMssv = request.getMssv().trim();
            String oldMssv = user.getMssv();

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
        user.setLocked(!user.isLocked());
        userRepository.save(user);
    }

    // --- [QUAN TRỌNG] HÀM MỚI ĐỂ XÓA USER VÀ DỮ LIỆU LIÊN QUAN ---
    @Transactional
    @Override
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 1. Xóa tất cả các vé (Registrations) của user này
        // (Đây là nguyên nhân gây lỗi 23503 của bạn)
        List<Registration> registrations = registrationRepository.findAllByUser(user);
        registrationRepository.deleteAll(registrations);

        notificationRepository.deleteAllByUserId(userId);

        // 2. Nếu User là Poster, cần xóa (hoặc xử lý) các sự kiện họ đã tạo
        // Nếu không xóa sự kiện, khi xóa user sẽ lại lỗi FK ở bảng events
        List<Event> events = eventRepository.findAllByNguoiDang(user);
        if (!events.isEmpty()) {
            // Trước khi xóa sự kiện, phải xóa các vé của sự kiện đó (của những sinh viên khác)
            for (Event event : events) {
                List<Registration> eventRegs = registrationRepository.findAllByEvent(event);
                registrationRepository.deleteAll(eventRegs);
            }
            // Sau đó xóa sự kiện
            eventRepository.deleteAll(events);
        }

        // 3. Cuối cùng mới xóa User
        userRepository.delete(user);
    }
    // -------------------------------------------------------------

    @Override
    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public UserResponse getMyProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));
        return convertToUserResponse(user);
    }

    @Override
    public UserResponse updateMyProfile(String userEmail, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        if (request.getEmail() != null && !request.getEmail().equals(userEmail)) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email mới đã được sử dụng");
            }
            user.setEmail(request.getEmail());
        }

        user.setSoDienThoai(request.getSoDienThoai());
        User updatedUser = userRepository.save(user);
        return convertToUserResponse(updatedUser);
    }

    @Override
    public void approveEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        event.setTrangThai(EventStatus.PUBLISHED);
        eventRepository.save(event);

        try {
            notificationService.createNotification(
                    event.getNguoiDang(),
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
        List<Event> events = eventRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return events.stream()
                .map(event -> convertToResponse(event, false))
                .collect(Collectors.toList());
    }

    // ... (Các hàm thống kê và xuất báo cáo giữ nguyên như file bạn gửi) ...
    // Để tiết kiệm không gian, tôi chỉ paste lại những phần đã sửa,
    // nhưng bạn hãy copy toàn bộ file này đè lên file cũ vì nó đã bao gồm đầy đủ imports và cấu trúc class.

    @Override
    public Map<String, Long> getTopEventStats(int year, int month) {
        LocalDateTime startDate;
        LocalDateTime endDate;

        if (month > 0 && month <= 12) {
            startDate = LocalDateTime.of(year, month, 1, 0, 0);
            endDate = startDate.plusMonths(1).minusNanos(1);
        } else {
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
        Map<Integer, Long> stats = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            stats.put(i, 0L);
        }
        for (Object[] row : results) {
            stats.put((Integer) row[0], (Long) row[1]);
        }
        return stats;
    }

    @Override
    public Map<String, Long> getTopCategoryStats(int year, int month) {
        LocalDateTime startDate;
        LocalDateTime endDate;

        if (month > 0 && month <= 12) {
            startDate = LocalDateTime.of(year, month, 1, 0, 0);
            endDate = startDate.plusMonths(1).minusNanos(1);
        } else {
            startDate = LocalDateTime.of(year, 1, 1, 0, 0);
            endDate = startDate.plusYears(1).minusNanos(1);
        }

        List<Object[]> results = registrationRepository.findCategoryStatsInDateRange(startDate, endDate);
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Object[] row : results) {
            stats.put((String) row[0], (Long) row[1]);
        }
        return stats;
    }

    @Override
    public byte[] exportDashboardReport(int year) throws IOException {
        Map<String, Long> topEvents = getTopEventStats(year, 0);
        Map<Integer, Long> monthlyStats = getMonthlyEventStats(year);
        Map<String, Long> categoryStats = getTopCategoryStats(year, 0);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Báo cáo Thống kê " + year);

        CellStyle titleStyle = workbook.createCellStyle();
        XSSFFont titleFont = (XSSFFont) workbook.createFont();
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

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

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        int rowNum = 0;

        Row mainTitleRow = sheet.createRow(rowNum++);
        Cell mainTitleCell = mainTitleRow.createCell(0);
        mainTitleCell.setCellValue("BÁO CÁO HOẠT ĐỘNG NĂM " + year);
        mainTitleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        rowNum++;

        rowNum = createSectionHeader(sheet, rowNum, "I. TOP SỰ KIỆN CÓ LƯỢT ĐĂNG KÝ CAO NHẤT", titleStyle);
        Row headerRow1 = sheet.createRow(rowNum++);
        createCell(headerRow1, 0, "Tên sự kiện", headerStyle);
        createCell(headerRow1, 1, "Số lượng đăng ký", headerStyle);

        for (Map.Entry<String, Long> entry : topEvents.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, entry.getKey(), dataStyle);
            createCell(row, 1, entry.getValue().toString(), dataStyle);
        }
        rowNum++;

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

        rowNum = createSectionHeader(sheet, rowNum, "III. THỐNG KÊ THEO CHỦ ĐỀ", titleStyle);
        Row headerRow3 = sheet.createRow(rowNum++);
        createCell(headerRow3, 0, "Chủ đề (Danh mục)", headerStyle);
        createCell(headerRow3, 1, "Số lượng", headerStyle);

        for (Map.Entry<String, Long> entry : categoryStats.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, entry.getKey(), dataStyle);
            createCell(row, 1, entry.getValue().toString(), dataStyle);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(0, 30 * 256);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private int createSectionHeader(Sheet sheet, int rowNum, String title, CellStyle style) {
        Row row = sheet.createRow(rowNum++);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        return rowNum;
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAllByDeletedAtIsNull();
        return categories.stream().map(cat -> {
            CategoryResponse dto = new CategoryResponse();
            dto.setId(cat.getId());
            dto.setTenDanhMuc(cat.getTenDanhMuc());
            long count = categoryRepository.countEventsByCategory(cat);
            dto.setSoLuongSuKien(count);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Category createCategory(Category category) {
        String tenClean = category.getTenDanhMuc().trim();
        if (categoryRepository.existsByTenDanhMucIgnoreCase(tenClean)) {
            throw new IllegalArgumentException("Tên danh mục này đã tồn tại!");
        }
        category.setTenDanhMuc(tenClean);
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục"));

        String oldName = category.getTenDanhMuc();
        String newName = categoryDetails.getTenDanhMuc().trim();

        if (!newName.equalsIgnoreCase(oldName) && categoryRepository.existsByTenDanhMucIgnoreCase(newName)) {
            throw new IllegalArgumentException("Tên danh mục '" + newName + "' đã tồn tại!");
        }

        category.setTenDanhMuc(newName);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        List<Event> events = eventRepository.findAllByCategoryId(id);
        for (Event event : events) {
            event.setCategory(null);
            eventRepository.save(event);
        }

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
        user.setDeletedAt(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void permanentDeleteUser(Long userId) {
        // A. Tìm user trong thùng rác
        User user = userRepository.findSoftDeletedById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user trong thùng rác"));

        // B. Tìm Admin để nhận sự kiện
        User adminUser = userRepository.findFirstByRole(Role.ADMIN);
        if (adminUser == null) {
            throw new RuntimeException("Hệ thống cần ít nhất 1 Admin để tiếp nhận sự kiện");
        }

        // C. Xóa vé & Thông báo
        registrationRepository.deleteAllByUserId(userId);
        notificationRepository.deleteAllByUserId(userId);

        // D. XỬ LÝ SỰ KIỆN (SỬA ĐOẠN NÀY)
        // ❌ Cũ: List<Event> events = eventRepository.findAllByNguoiDang(user); -> Bỏ dòng này

        // ✅ Mới: Lấy tất cả sự kiện, KỂ CẢ SỰ KIỆN TRONG THÙNG RÁC
        List<Event> events = eventRepository.findAllByNguoiDangIdIncludingDeleted(userId);

        if (!events.isEmpty()) {
            for (Event event : events) {
                // Logic: Chỉ giữ lại sự kiện ĐÃ PUBLISHED và CHƯA BỊ XÓA MỀM
                // (Sự kiện trong thùng rác dù đã Publish trước đó cũng nên xóa luôn cho sạch)
                boolean isPublishedAndActive = event.getTrangThai() == EventStatus.PUBLISHED && event.getDeletedAt() == null;

                if (isPublishedAndActive) {
                    // ==> TRƯỜNG HỢP 1: Sự kiện tốt -> CHUYỂN CHO ADMIN
                    event.setNguoiDang(adminUser);
                    eventRepository.save(event);
                } else {
                    // ==> TRƯỜNG HỢP 2: Nháp, Hủy, hoặc ĐANG TRONG THÙNG RÁC -> XÓA VĨNH VIỄN

                    // 1. Xóa vé của sự kiện này (dùng native query cho chắc chắn)
                    registrationRepository.deleteRegistrationsByEventId(event.getId());

                    // 2. Xóa sự kiện vĩnh viễn
                    eventRepository.permanentDelete(event.getId());
                }
            }
        }

        // E. Cuối cùng: Xóa vĩnh viễn User
        userRepository.permanentDelete(userId);
    }

    // --- Event Recycle Bin ---

    @Override
    public List<EventResponse> getDeletedEvents() {
        return eventRepository.findSoftDeleted().stream()
                .map(event -> convertToResponse(event, false))
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

        if (registrationRepository.countByEvent(event) > 0) {
            // Hoặc bạn có thể cho phép xóa luôn vé tại đây:
            // registrationRepository.deleteAllByEvent(event);
            // eventRepository.permanentDelete(eventId);
            throw new IllegalStateException("Không thể xóa vĩnh viễn. Sự kiện này đã có người đăng ký.");
        }

        eventRepository.permanentDelete(eventId);
    }

    // ... (Phần Category Trash, Banner Trash, Dashboard Activity giữ nguyên)
    // 3. Category Trash
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
    @Transactional
    public void hardDeleteCategory(Long id) {
        // 1. Tìm danh mục trong thùng rác (để chắc chắn nó tồn tại)
        // (Hoặc tìm bằng findById nếu bạn cho phép xóa cứng trực tiếp không qua thùng rác)
        Category category = categoryRepository.findSoftDeletedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục trong thùng rác"));

        // 2. [QUAN TRỌNG] Gỡ danh mục này ra khỏi TẤT CẢ sự kiện (Set về NULL)
        // Nếu không làm bước này, DB sẽ báo lỗi Foreign Key
        eventRepository.unlinkCategory(id);

        // 3. Xóa vĩnh viễn danh mục
        categoryRepository.permanentDelete(id);
    }

    // 4. Banner Trash
    @Override
    public List<Banner> getDeletedBanners() {
        return bannerRepository.findSoftDeleted();
    }

    @Override
    public Banner restoreBanner(Long id) {
        bannerRepository.restoreBanner(id);
        return bannerRepository.findById(id).orElse(null);
    }

    // Hàm xóa file an toàn tuyệt đối
    private void deleteFile(String fileName) {
        // 1. Kiểm tra rỗng
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }

        // 2. Kiểm tra nếu là link online (http/https) -> BỎ QUA NGAY
        if (fileName.toLowerCase().startsWith("http://") || fileName.toLowerCase().startsWith("https://")) {
            System.out.println("DEBUG: Bỏ qua xóa file vì là URL online: " + fileName);
            return;
        }

        // 3. Cố gắng xóa file vật lý
        try {
            // Sử dụng Paths.get có thể gây lỗi nếu chuỗi chứa ký tự lạ, nên bọc try-catch lớn
            java.nio.file.Path rootPath = java.nio.file.Paths.get("uploads").toAbsolutePath().normalize();
            java.nio.file.Path filePath = rootPath.resolve(fileName).normalize();

            // Kiểm tra file có tồn tại không trước khi xóa
            if (java.nio.file.Files.exists(filePath)) {
                java.nio.file.Files.delete(filePath);
                System.out.println("DEBUG: Đã xóa file vật lý thành công: " + fileName);
            } else {
                System.out.println("DEBUG: File không tồn tại trên ổ cứng (Bỏ qua): " + fileName);
            }
        } catch (Exception e) {
            // QUAN TRỌNG: Chỉ in log, KHÔNG ĐƯỢC ném ngoại lệ (throw) ra ngoài
            // Nếu throw ở đây, Transaction sẽ rollback và Database sẽ không bị xóa.
            System.err.println("WARN: Lỗi không xóa được file (nhưng vẫn sẽ xóa DB): " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void hardDeleteBanner(Long id) {
        // 1. Tìm Banner trong thùng rác để lấy URL ảnh
        // Dùng native query của repo để tìm, tránh bị bộ lọc @Where chặn
        Banner banner = bannerRepository.findSoftDeletedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy banner trong thùng rác"));

        // 2. Xóa file ảnh (Sử dụng hàm deleteFile an toàn đã viết trước đó)
        deleteFile(banner.getImageUrl());

        // 3. [QUAN TRỌNG] Xóa vĩnh viễn bằng EntityManager
        // Cách này đi đường vòng, bỏ qua Hibernate @SQLDelete để xóa thật trong DB
        entityManager.createNativeQuery("DELETE FROM banners WHERE id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public Map<String, Long> getDashboardStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalEvents", eventRepository.count());
        stats.put("totalRegistrations", registrationRepository.count());
        return stats;
    }

    @Override
    public Map<String, Long> getEventRegistrationStats() {
        List<Object[]> results = eventRepository.findTop5EventsByRegistration();
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

        List<Event> recentEvents = eventRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        for (Event e : recentEvents) {
            try {
                String tenNguoiDang = e.getNguoiDang() != null ? e.getNguoiDang().getHoTen() : "Người dùng đã xóa";
                activities.add(new DashboardActivity(
                        "Sự kiện \"" + e.getTieuDe() + "\" đã được tạo bởi " + tenNguoiDang,
                        e.getCreatedAt(),
                        "create_event"
                ));
            } catch (EntityNotFoundException ex) {
                activities.add(new DashboardActivity(
                        "Sự kiện \"" + e.getTieuDe() + "\" (Người đăng đã bị xóa)",
                        e.getCreatedAt(),
                        "create_event"
                ));
            }
        }

        List<Registration> recentRegs = registrationRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        for (Registration r : recentRegs) {
            try {
                String tenSinhVien = r.getUser() != null ? r.getUser().getHoTen() : "SV đã xóa";
                String tenSuKien = r.getEvent() != null ? r.getEvent().getTieuDe() : "Sự kiện đã xóa";

                activities.add(new DashboardActivity(
                        tenSinhVien + " đã đăng ký tham gia \"" + tenSuKien + "\".",
                        r.getCreatedAt(),
                        "register"
                ));
            } catch (EntityNotFoundException ex) {
                // Skip if related data is missing
            }
        }

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