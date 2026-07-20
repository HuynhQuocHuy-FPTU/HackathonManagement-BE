package com.hackathon.security;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;


@Component
@Order(1) // Đảm bảo Filter này chạy trước các Filter xử lý Multipart khác
public class FileSizeFilter implements Filter {

    // Content-Length là kích thước toàn bộ request, không phải từng file.
    private static final long MAX_REQUEST_SIZE = 100L * 1024 * 1024;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Chỉ kiểm tra trên endpoint upload file
        if (httpRequest.getRequestURI().contains("/api/submissions/")
                && httpRequest.getContentLengthLong() > MAX_REQUEST_SIZE) {
            httpResponse.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            httpResponse.setContentType("application/json");
            httpResponse.setCharacterEncoding("UTF-8");
            httpResponse.getWriter().write(
                    "{\"success\":false,\"message\":\"Tổng dung lượng request vượt quá giới hạn 100MB.\"}"
            );
            return; // Chặn request tại đây, không cho vào Controller
        }

        chain.doFilter(request, response); // Cho phép request đi tiếp nếu hợp lệ
    }
}
