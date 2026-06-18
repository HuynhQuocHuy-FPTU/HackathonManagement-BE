package com.hackathon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter này sẽ chạy MỘT LẦN duy nhất cho mỗi Request gửi đến Server.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    // TỐI ƯU: Sử dụng UserDetailsService thay vì chọc trực tiếp vào AccountRepository
    private final CustomUserDetailsService userDetailsService;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lấy chuỗi Token từ HTTP Header
        String authHeader = request.getHeader("Authorization");

        // 2. Nếu không có Header hoặc Header không bắt đầu bằng "Bearer ", bỏ qua Filter này và đi tiếp (sẽ bị chặn ở vòng ngoài nếu API cần bảo mật)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Cắt bỏ 7 ký tự "Bearer " để lấy đúng chuỗi mã hóa JWT
        String jwt = authHeader.substring(7);

        try {
            // 4. Giải mã lấy email
            String email = jwtService.extractEmail(jwt);

            // 5. Nếu lấy được email và hiện tại Spring Security chưa ghi nhận ai đang đăng nhập
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 6. Gọi chuyên viên tra cứu hồ sơ lấy thông tin User từ DB
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);

                // 7. Máy soi chiếu kiểm tra Token xem có phải đồ giả hoặc hết hạn không
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // 8. Token chuẩn -> Đóng dấu hợp lệ, cấp quyền đi tiếp
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Không cần password ở bước này
                            userDetails.getAuthorities() // Nạp danh sách quyền (Roles)
                    );

                    // Lưu lại các thông tin chi tiết của Request (như IP address, Session Id)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Lưu vào Context của Spring, thông báo: "Người này đã xác thực thành công!"
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                }
            }
        } catch (Exception ignored) {
            // Nếu có bất kỳ lỗi gì (Token bị sửa đổi, hết hạn, hoặc User bị xóa khỏi DB),
            // ta ngó lơ nó đi. Request sẽ bị đánh dấu là "Chưa đăng nhập" (Unauthenticated)
            // và Spring Security sẽ tự động ném ra lỗi 403 hoặc 401.
            log.error("Lỗi xác thực JWT: {}", ignored.getMessage());

        }

        // 9. Cho phép Request đi tiếp tới Filter tiếp theo hoặc tới Controller
        filterChain.doFilter(request, response);
    }
}
