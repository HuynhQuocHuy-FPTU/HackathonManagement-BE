package com.hackathon.service.submission;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hackathon.entity.enums.FileType;
import com.hackathon.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {
    private final Cloudinary cloudinary;

    public String uploadFile(MultipartFile file, FileType fileType) {
        Map<String, Object> params = ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "hackathon_submissions"
        );

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            // Kiểm tra nếu nguyên nhân là do lỗi timeout mạng
            if (e.getCause() instanceof java.net.SocketTimeoutException) {
                log.error("Cloudinary upload timed out: {}", e.getMessage());
                throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "Đường truyền quá chậm, không thể hoàn tất upload.");
            }
            // Các lỗi IOException khác
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi upload lên Cloudinary: " + e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi upload Cloudinary chi tiết: ", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi tải file lên hệ thống lưu trữ: " + e.getMessage());
        }
    }

    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.error("Lỗi xóa file Cloudinary chi tiết: ", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi xóa file trên hệ thống lưu trữ!");
        }
    }
}

