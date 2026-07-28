package com.hackathon.service.systemConfig;

import com.hackathon.entity.SystemConfig;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.SystemConfigKey;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.SystemConfigRepository;
import com.hackathon.security.CustomUserDetails;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;


    // Admin cấu hình lúc tạo Team có bao nhiêu thành viên tối đa bao nhiêu người được mời tham gia
    @Override
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
