package com.hackathon.service.impl;

import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.enums.AuditAction;
import com.hackathon.entity.enums.AuditEntityType;
import com.hackathon.entity.enums.WorkshopStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.WorkshopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Cài đặt các nghiệp vụ dùng để hoàn thành hoặc hủy workshop của một sự kiện.
@Service
@RequiredArgsConstructor
public class WorkshopServiceImpl implements WorkshopService {

    private final HackathonEventRepository eventRepository;

    private final AuditService auditService;

    // Chuyển workshop sang trạng thái đã hoàn thành.
    // Chỉ workshop đang diễn ra mới được phép chuyển sang trạng thái này.
    @Override
    @Transactional
    public void completedWorkshop(Integer eventId, CustomUserDetails userDetails) {
        // Tìm sự kiện cần cập nhật và dừng xử lý nếu mã sự kiện không tồn tại.
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        // Không cho phép hoàn thành lại workshop đã được đánh dấu hoàn thành trước đó.
        if (event.getWorkshopStatus() == WorkshopStatus.COMPLETED) {
            throw new BadRequestException("Workshop đã hoàn thành rồi, không thể cập nhật thêm!");
        }

        // Workshop đã hủy không thể được chuyển sang trạng thái hoàn thành.
        if (event.getWorkshopStatus() == WorkshopStatus.CANCELLED) {
            throw new BadRequestException("Workshop này đã bị hủy, không thể đánh dấu hoàn thành!");
        }

        // Không cho phép kết thúc workshop khi workshop vẫn chưa bắt đầu.
        if (event.getWorkshopStatus() == WorkshopStatus.UPCOMING) {
            throw new BadRequestException("Workshop chưa bắt đầu, bạn không thể kết thúc sớm!");
        }

        // Sau khi vượt qua các điều kiện trên, workshop đang diễn ra và có thể hoàn thành.
        event.setWorkshopStatus(WorkshopStatus.COMPLETED);

        // Lưu trạng thái mới của workshop vào cơ sở dữ liệu.
        eventRepository.save(event);

        // Ghi lại người thực hiện và nội dung thay đổi để phục vụ việc kiểm tra lịch sử.
        auditService.saveLog(
                userDetails.getAccount(),
                AuditAction.UPDATE_EVENT,
                AuditEntityType.EVENT,
                eventId,
                "Completed workshop of " + event.getEventName()
        );
    }

    // Chuyển workshop sang trạng thái đã hủy.
    // Workshop sắp diễn ra hoặc đang diễn ra đều có thể được hủy.
    @Override
    @Transactional
    public void cancelWorkshop(Integer eventId, CustomUserDetails userDetails) {
        // Tìm sự kiện cần hủy workshop và báo lỗi nếu sự kiện không tồn tại.
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        // Workshop đã hoàn thành là kết quả cuối cùng nên không thể hủy lại.
        if (event.getWorkshopStatus() == WorkshopStatus.COMPLETED) {
            throw new BadRequestException("Không thể hủy Workshop đã hoàn thành!");
        }

        // Ngăn việc gửi yêu cầu hủy nhiều lần cho cùng một workshop.
        if (event.getWorkshopStatus() == WorkshopStatus.CANCELLED) {
            throw new BadRequestException("Workshop này đã ở trạng thái hủy từ trước rồi!");
        }

        // Các trạng thái còn lại là UPCOMING hoặc ONGOING nên được phép hủy.
        event.setWorkshopStatus(WorkshopStatus.CANCELLED);

        // Lưu trạng thái đã hủy của workshop vào cơ sở dữ liệu.
        eventRepository.save(event);

        // Ghi nhật ký thao tác hủy để xác định người thực hiện và sự kiện bị tác động.
        auditService.saveLog(
                userDetails.getAccount(),
                AuditAction.UPDATE_EVENT,
                AuditEntityType.EVENT,
                eventId,
                "Cancelled workshop of " + event.getEventName()
        );
    }
}
