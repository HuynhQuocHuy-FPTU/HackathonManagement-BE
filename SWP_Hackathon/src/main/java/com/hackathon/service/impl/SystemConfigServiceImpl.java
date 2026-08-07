package com.hackathon.service.impl;

import com.hackathon.entity.SystemConfig;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.SystemConfigKey;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.SystemConfigRepository;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// Đọc và cập nhật các giá trị cấu hình dùng chung của hệ thống.
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;


    // Admin cấu hình lúc tạo Team có bao nhiêu thành viên tối đa bao nhiêu người được mời tham gia
    @Override
    // Kiểm tra quyền quản trị rồi cập nhật giá trị của khóa cấu hình được chọn.
    public void updateSystemConfig(CustomUserDetails userDetails, Integer value, SystemConfigKey key) {
        if (userDetails.getAccount().getRole() != AccountRole.ADMIN) {
            throw new BadRequestException("Bạn không phải là ADMIN , bạn không có quyền để truy cập.");
        }

        if (value <= 0) {
            throw new BadRequestException("Kích thược đội không được nhỏ hơn hoặc bằng 0.");
        }

        SystemConfig config = systemConfigRepository
                .findByConfigKey(key.name())
                .orElse(new SystemConfig());

        config.setConfigValue(String.valueOf(value));
        config.setConfigKey(key.name());
        systemConfigRepository.save(config);

    }

    // Đọc cấu hình dạng số nguyên và báo lỗi nếu khóa chưa được thiết lập đúng.
    public int getIntConfig(SystemConfigKey key) {

        SystemConfig config = systemConfigRepository
                .findByConfigKey(key.name())
                .orElseThrow(() ->
                        new BadRequestException(
                                "Chưa cấu hình " + key
                        )
                );

        return Integer.parseInt(config.getConfigValue());
    }

}
