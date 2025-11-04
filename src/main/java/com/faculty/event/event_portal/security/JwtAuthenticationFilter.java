package com.faculty.event.event_portal.security;

import com.faculty.event.event_portal.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // Đánh dấu đây là 1 Bean để Spring quản lý
public class JwtAuthenticationFilter extends OncePerRequestFilter { // Đảm bảo filter chạy 1 LẦN cho mỗi request

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService; // Đây chính là UserDetailsServiceImpl

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lấy header 'Authorization' từ request
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Kiểm tra xem header có tồn tại và có 'Bearer ' không
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Cho request đi tiếp (sẽ bị chặn sau nếu API yêu cầu)
            return;
        }

        // 3. Tách lấy token (bỏ chữ "Bearer ")
        jwt = authHeader.substring(7); // "Bearer ".length() == 7

        // 4. Dùng JwtService để trích xuất email từ token
        userEmail = jwtService.extractUsername(jwt);

        // 5. Kiểm tra email có tồn tại và user chưa được xác thực
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Lấy thông tin user từ CSDL
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 7. Kiểm tra token có hợp lệ không
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // 8. Nếu hợp lệ, tạo 1 token xác thực và đưa user vào SecurityContext
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // Không cần credentials (password)
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 9. Cập nhật SecurityContextHolder
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 10. Cho request đi tiếp
        filterChain.doFilter(request, response);
    }
}