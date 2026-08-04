package com.hackathon.service;

import com.hackathon.entity.enums.SystemConfigKey;
import com.hackathon.security.CustomUserDetails;

public interface SystemConfigService {
    void updateSystemConfig(CustomUserDetails userDetails, Integer maxSizeTeam, SystemConfigKey key);

    int getIntConfig(SystemConfigKey key);
}
