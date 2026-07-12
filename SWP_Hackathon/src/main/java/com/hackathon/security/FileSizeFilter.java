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

    // Đặt ngưỡng giới hạn ở đây (Ví dụ: 80MB)
    private static final long MAX_FILE_SIZE = 60 * 1024 * 1024;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Chỉ kiểm tra trên endpoint upload file
        if (httpRequest.getRequestURI().contains("/api/submissions/") && httpRequest.getContentLength() > MAX_FILE_SIZE) {
            httpResponse.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            httpResponse.getWriter().write("{\"success\": false, \"message\": \"File quá lớn! Giới hạn là 80MB.\"}");
            return; // Chặn request tại đây, không cho vào Controller
        }

        chain.doFilter(request, response); // Cho phép request đi tiếp nếu hợp lệ
    }
}