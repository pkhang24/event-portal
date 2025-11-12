package com.faculty.event.event_portal.service.impl;

import com.faculty.event.event_portal.dto.AuthResponse;
import com.faculty.event.event_portal.dto.LoginRequest;
//import com.faculty.event.event_portal.dto.RegisterRequest;
//import com.faculty.event.event_portal.entity.Role;
import com.faculty.event.event_portal.entity.User;
import com.faculty.event.event_portal.repository.UserRepository;
import com.faculty.event.event_portal.service.AuthService;
import com.faculty.event.event_portal.service.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    // Tiêm (Inject) các dependency cần thiết
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

//    @Override
//    public AuthResponse register(RegisterRequest request) {
//        // 1. Kiểm tra xem email đã tồn tại chưa
//        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
//            // Ném ra lỗi nếu email đã tồn tại
//            throw new IllegalArgumentException("Email đã được sử dụng");
//        }
//
//        // 2. Tạo một đối tượng User mới
//        User newUser = new User();
//        newUser.setHoTen(request.getHoTen());
//        newUser.setMssv(request.getMssv());
//        newUser.setEmail(request.getEmail());
//        // 3. Mã hóa mật khẩu
//        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
//        // 4. Gán vai trò mặc định là STUDENT
//        newUser.setRole(Role.STUDENT);
//
//        // 5. Lưu user vào CSDL
//        userRepository.save(newUser);
//
//        // 6. Tạo JWT token cho user mới
//        // (Chúng ta cần tạo một đối tượng UserDetails cho JwtService)
//        var userDetails = org.springframework.security.core.userdetails.User
//                .withUsername(newUser.getEmail())
//                .password(newUser.getPassword())
//                .authorities(newUser.getRole().name()) // Dùng .name() để lấy tên Enum
//                .build();
//
//        String token = jwtService.generateToken(userDetails);
//
//        // 7. Trả về token
//        return new AuthResponse(token);
//    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Lấy User entity đầy đủ (thay vì UserDetails)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user"));

        // Gọi hàm generateToken MỚI
        String token = jwtService.generateToken(user);

        return new AuthResponse(token);
    }
}