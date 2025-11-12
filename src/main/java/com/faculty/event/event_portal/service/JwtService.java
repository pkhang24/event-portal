package com.faculty.event.event_portal.service;

import com.faculty.event.event_portal.entity.User; // Import User entity của bạn
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    // Tạo một key bí mật (ít nhất 256 bit) và lưu vào application.properties
    // Ví dụ: jwt.secret=daylamotkeybimatratdainvaduynhatcuaban123456789
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    // Trích xuất email từ token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Trích xuất 1 claim
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Code MỚI (đã bổ sung ROLE)
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Lấy role từ UserDetails và đưa vào claims
        // Vì userDetails.getAuthorities() trả về một danh sách, ta lấy phần tử đầu tiên
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("STUDENT"); // Giá trị mặc định nếu không tìm thấy

        claims.put("role", role); // <-- QUAN TRỌNG NHẤT: Thêm dòng này

        return createToken(claims, userDetails.getUsername());
    }

    // --- HÀM MỚI (chấp nhận User entity) ---
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();

        // Thêm các claims tùy chỉnh
        claims.put("role", user.getRole().name());
        claims.put("hoTen", user.getHoTen()); // <-- ĐÂY LÀ DÒNG MỚI QUAN TRỌNG

        return createToken(claims, user.getEmail()); // Dùng email làm subject
    }

    // Hàm createToken (nếu bạn tách riêng ra)
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims) // <-- Đảm bảo có dòng này để đưa claims vào token
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

//    // Tạo token
//    public String generateToken(UserDetails userDetails) {
//        return Jwts.builder()
//                .setSubject(userDetails.getUsername())
//                .setIssuedAt(new Date(System.currentTimeMillis()))
//                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 giờ
//                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
//                .compact();
//    }

    // Kiểm tra token
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        // Chuyển đổi key (văn bản thuần) sang mảng byte
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        // Dùng mảng byte đó để tạo key
        return Keys.hmacShaKeyFor(keyBytes);
    }
}