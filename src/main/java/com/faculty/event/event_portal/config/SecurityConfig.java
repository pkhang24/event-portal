package com.faculty.event.event_portal.config;

// (Hãy giữ các import cũ của bạn)
import com.faculty.event.event_portal.entity.Role;
import com.faculty.event.event_portal.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
// <<<--- THÊM CÁC IMPORT SAU ---
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // <<< ĐẢM BẢO BẠN CÓ ANNOTATION NÀY
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
// --- HẾT PHẦN THÊM IMPORT ---
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import java.util.List;

@Configuration
@EnableWebSecurity // <<< ĐẢM BẢO BẠN CÓ ANNOTATION NÀY
public class SecurityConfig {

    // --- BƯỚC 1: TIÊM FILTER VÀO CLASS ---
    private final JwtAuthenticationFilter jwtAuthFilter;
    // (Nếu bạn có các service khác được tiêm, hãy giữ chúng)

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter
            /*, các service khác... */) { // <<< CẬP NHẬT CONSTRUCTOR
        this.jwtAuthFilter = jwtAuthFilter;
        // (Gán các service khác ở đây)
    }

    // --- BƯỚC 2: THÊM CÁC BEAN CÒN THIẾU ---

    // Bean 1: Định nghĩa bộ mã hóa mật khẩu (Sửa lỗi của bạn)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Bean 2: Định nghĩa bộ quản lý xác thực (AuthServiceImpl cần)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    // --- BƯỚC 3: BEAN FILTER CHAIN (GIỮ NGUYÊN) ---
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF (cú pháp mới)
                .csrf(csrf -> csrf.disable())

                // 2. Cấu hình CORS (cú pháp mới)
                // ...
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new CorsConfiguration();
                    // Cho phép cả localhost VÀ địa chỉ IP mạng của bạn
                    corsConfig.setAllowedOrigins(List.of(
                            "http://localhost:5173",
                            "http://192.168.2.5:5173" // <<<--- THÊM DÒNG NÀY
                    ));
                    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(List.of("*"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))
// ...

                // 3. Cấu hình Session (cú pháp mới)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. Phân quyền API (giữ nguyên, không .and())
                .authorizeHttpRequests(authorize -> authorize
                        // --- CÁC ENDPOINT PUBLIC (AI CŨNG VÀO ĐƯỢC) ---
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/banners/**").permitAll()

                        // --- CÁC ENDPOINT CỦA STUDENT ---
                        .requestMatchers("/api/registrations/my-tickets").hasAuthority(Role.STUDENT.name())
                        .requestMatchers(HttpMethod.POST, "/api/registrations").hasAuthority(Role.STUDENT.name())

                        // --- CÁC ENDPOINT CỦA POSTER ---
                        .requestMatchers(HttpMethod.POST, "/api/events").hasAuthority(Role.POSTER.name())
                        .requestMatchers(HttpMethod.PUT, "/api/events/**").hasAuthority(Role.POSTER.name())
                        .requestMatchers("/api/registrations/check-in").hasAnyAuthority(Role.POSTER.name(), Role.ADMIN.name())
                        // Trong SecurityConfig.java, thêm vào mục POSTER:
                        .requestMatchers(HttpMethod.GET, "/api/events/my-events").hasAuthority(Role.POSTER.name())

                        // --- CÁC ENDPOINT CỦA ADMIN ---
                        .requestMatchers("/api/admin/**").hasAuthority(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasAuthority(Role.ADMIN.name())

                        // Tất cả các request khác đều cần đăng nhập
                        .anyRequest().authenticated()
                )

                // Kích hoạt filter JWT
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}