package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.dto.CreateUserRequest;
import com.faculty.event.event_portal.dto.EventResponse;
import com.faculty.event.event_portal.dto.UpdateProfileRequest;
import com.faculty.event.event_portal.dto.UserResponse;
import com.faculty.event.event_portal.entity.Event;
import com.faculty.event.event_portal.entity.EventStatus;
import com.faculty.event.event_portal.entity.Role;
import com.faculty.event.event_portal.entity.User;
import com.faculty.event.event_portal.repository.EventRepository;
import com.faculty.event.event_portal.repository.RegistrationRepository;
import com.faculty.event.event_portal.repository.UserRepository;
import com.faculty.event.event_portal.service.AdminService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                            PasswordEncoder passwordEncoder) { // Thêm
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.passwordEncoder = passwordEncoder; // Thêm
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setHoTen(user.getHoTen());
        response.setEmail(user.getEmail());
        response.setMssv(user.getMssv());
        response.setRole(user.getRole().name());
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