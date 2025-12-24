package com.faculty.event.event_portal.config;

import com.faculty.event.event_portal.entity.Role;
import com.faculty.event.event_portal.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter)
    {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new CorsConfiguration();
                    corsConfig.setAllowedOrigins(List.of(
                            "http://localhost:5173",
                            "http://192.168.2.8:5173"
                    ));
                    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(List.of("*"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))

                // Cấu hình Session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Phân quyền API
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/events/my-events").hasAuthority(Role.POSTER.name()) // <--- QUAN TRỌNG
                        .requestMatchers(HttpMethod.GET, "/api/events/my-trash").hasAuthority(Role.POSTER.name())  // <--- THÊM CÁI NÀY NỮA
                        .requestMatchers(HttpMethod.GET, "/api/events/*/participants").hasAnyAuthority(Role.POSTER.name(), Role.ADMIN.name())

                        // CÁC API CỤ THỂ CỦA ADMIN
                        .requestMatchers("/api/admin/**").hasAuthority(Role.ADMIN.name())

                        // CÁC API PUBLIC
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // Cho phép GET chi tiết sự kiện và danh sách public
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/banners/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                        // --- CÁC ENDPOINT CỦA POSTER ---
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