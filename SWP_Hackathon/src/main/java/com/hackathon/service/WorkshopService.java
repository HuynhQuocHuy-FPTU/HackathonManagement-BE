package com.hackathon.service;

import com.hackathon.security.CustomUserDetails;

public interface WorkshopService {

    // Đánh dấu workshop của sự kiện đã hoàn thành khi workshop đang diễn ra.
    void completedWorkshop(Integer eventId, CustomUserDetails userDetails);

    // Hủy workshop của sự kiện khi workshop chưa hoàn thành hoặc chưa bị hủy trước đó.
    void cancelWorkshop(Integer eventId, CustomUserDetails userDetails);
}
