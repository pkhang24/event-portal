package com.faculty.event.event_portal.service.impl;

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
import org.springframework.stereotype.Service;

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

    public AdminServiceImpl(UserRepository userRepository,
                            EventRepository eventRepository,
                            RegistrationRepository registrationRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
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
    public void approveEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện"));

        event.setTrangThai(EventStatus.PUBLISHED);
        eventRepository.save(event);
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