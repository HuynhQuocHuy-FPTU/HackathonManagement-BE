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
        // Sử dụng "auto" để Cloudinary tự nhận diện định dạng file (tránh lỗi thủ công)
        Map<String, Object> params = ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "hackathon_submissions"
        );

        try {
            // Sử dụng getBytes() để tránh lỗi InputStream bị đóng bất ngờ
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            // Log chi tiết lỗi để bạn thấy trên console
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

