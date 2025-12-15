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
                            "http://192.168.2.8:5173" // <<<--- THÊM DÒNG NÀY
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
                        // 1. CÁC API CỤ THỂ CỦA POSTER (ĐƯA LÊN ĐẦU TIÊN)
                        // Phải đặt cái này TRƯỚC dòng .permitAll() bên dưới
                        .requestMatchers(HttpMethod.GET, "/api/events/my-events").hasAuthority(Role.POSTER.name()) // <--- QUAN TRỌNG
                        .requestMatchers(HttpMethod.GET, "/api/events/my-trash").hasAuthority(Role.POSTER.name())  // <--- THÊM CÁI NÀY NỮA
                        .requestMatchers(HttpMethod.GET, "/api/events/*/participants").hasAnyAuthority(Role.POSTER.name(), Role.ADMIN.name())

                        // 2. CÁC API CỤ THỂ CỦA ADMIN
                        .requestMatchers("/api/admin/**").hasAuthority(Role.ADMIN.name())

                        // 3. SAU ĐÓ MỚI ĐẾN CÁC API PUBLIC (Wildcard)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // Cho phép GET chi tiết sự kiện và danh sách public
                        // NHƯNG loại trừ các cái đã định nghĩa ở trên (do thứ tự ưu tiên)
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/banners/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                        // --- CÁC ENDPOINT CỦA POSTER (CÁC METHOD KHÁC) ---
                        .requestMatchers(HttpMethod.POST, "/api/events").hasAuthority(Role.POSTER.name())
                        .requestMatchers(HttpMethod.PUT, "/api/events/**").hasAuthority(Role.POSTER.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasAnyAuthority(Role.POSTER.name(), Role.ADMIN.name())
                        .requestMatchers("/api/registrations/check-in").hasAuthority(Role.POSTER.name())
                        .requestMatchers(HttpMethod.POST, "/api/events/*/restore").hasAnyAuthority(Role.POSTER.name(), Role.ADMIN.name()) // Restore
                        .requestMatchers(HttpMethod.DELETE, "/api/events/*/permanent").hasAnyAuthority(Role.POSTER.name(), Role.ADMIN.name()) // Permanent Delete

                        // --- CÁC ENDPOINT CỦA STUDENT ---
                        .requestMatchers("/api/registrations/my-tickets").hasAuthority(Role.STUDENT.name())
                        .requestMatchers(HttpMethod.POST, "/api/registrations").hasAuthority(Role.STUDENT.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/registrations/**").hasAuthority(Role.STUDENT.name())

                        // Tất cả các request khác đều cần đăng nhập
                        .anyRequest().authenticated()
                )

                // Kích hoạt filter JWT
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}