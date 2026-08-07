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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {
    private final Cloudinary cloudinary;

    public String uploadFile(MultipartFile file, FileType fileType) {
        Map<String, Object> params = new HashMap<>();
        params.put("folder", "hackathon_submissions");

        // Tệp tài liệu và tệp nén được lưu dưới dạng raw để giữ nguyên nội dung, tên và phần mở rộng.
        if (fileType.getGroup() == FileType.FileGroup.DOCUMENT
                || fileType.getGroup() == FileType.FileGroup.ARCHIVE) {
            params.put("resource_type", "raw");

            // Thư mục ngẫu nhiên tránh ghi đè, còn public_id giữ tên gốc để tải về đúng tên file.
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && !originalFilename.isBlank()) {
                params.put("public_id", UUID.randomUUID() + "/" + originalFilename);
            }
        } else {
            params.put("resource_type", "auto");
        }

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

    /**
     * Hàm tạo riêng  để upload file Excel hệ thống
     */
    public String uploadExcelFile(MultipartFile file) {
        // Ép cứng resource_type là raw để không bị lỗi hỏng cấu trúc file Excel (.xlsx)
        Map<String, Object> params = new HashMap<>();
        params.put("resource_type", "raw");
        params.put("folder", "hackathon_rankings");

        if (file.getOriginalFilename() != null) {
            params.put("public_id", file.getOriginalFilename()); // Truyền trọn vẹn tên kèm đuôi .xlsx
        }
        try {
            log.info("Bắt đầu upload file Excel hệ thống lên Cloudinary: {}", file.getOriginalFilename());
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            if (e.getCause() instanceof java.net.SocketTimeoutException) {
                log.error("Cloudinary upload Excel timed out: {}", e.getMessage());
                throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "Đường truyền quá chậm, không thể hoàn tất upload file Excel.");
            }
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi kết nối Cloudinary khi upload Excel: " + e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi upload Excel Cloudinary chi tiết: ", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi tải file Excel lên hệ thống lưu trữ: " + e.getMessage());
        }
    }

}

