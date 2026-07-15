package com.hackathon.security;

import com.hackathon.config.JwtProperties;
import com.hackathon.entity.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Service độc lập chuyên xử lý mọi nghiệp vụ liên quan đến Token.
 */
@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Tạo Access Token khi người dùng đăng nhập thành công.
     */
    public String generateAccessToken(Account account) {
        return buildToken(account, jwtProperties.getAccessExpirationMs());
    }

    /**
     * Tạo Refresh Token (Chỉ là một chuỗi UUID ngẫu nhiên lưu xuống DB, không cần mã hóa JWT)
     */
    public String generateRefreshTokenValue() {
        return UUID.randomUUID().toString();
    }

    public long getAccessExpirationMs() {
        return jwtProperties.getAccessExpirationMs();
    }

    public long getRefreshExpirationMs() {
        return jwtProperties.getRefreshExpirationMs();
    }

    /**
     * Giải mã Token và lấy ra Subject (Email).
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Kiểm tra xem Token còn hợp lệ không (So sánh email trong token với email thực tế và kiểm tra hạn sử dụng).
     * @Note: Đã sửa tham số Account thành UserDetails để chuẩn hóa với Spring Security Filter.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Hàm nội bộ: Xây dựng chuỗi JWT với thư viện 0.12.x
     */
    private String buildToken(Account account, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(account.getEmail())
                // Custom Claims: Gắn thêm ID và Role để Frontend tiện sử dụng mà không cần gọi thêm API
                .claim("accountId", account.getAccountId())
                .claim("role", account.getRole().name())
                .claim("accountStatus", account.getStatus().name())
                .claim("githubId", account.getGithubId())
                .claim("githubUsername", account.getGithubUsername())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey()) // Tự động nhận diện thuật toán mã hóa từ độ dài của Key
                .compact();
    }

    /**
     * Hàm nội bộ: Giải mã và xác thực chữ ký của Token
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Mở khóa bằng SecretKey
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Hàm nội bộ: Kiểm tra xem thời gian hiện tại đã vượt qua thời gian Expiration của Token chưa
     */
    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    /**
     * Hàm nội bộ: Chuyển đổi chuỗi String bí mật thành đối tượng SecretKey chuẩn của Java
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
