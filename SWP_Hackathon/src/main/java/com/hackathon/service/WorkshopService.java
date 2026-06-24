package com.hackathon.service;

import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.enums.AuditAction;
import com.hackathon.entity.enums.AuditEntityType;
import com.hackathon.entity.enums.WorkshopStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkshopService {

    private final HackathonEventRepository eventRepository;
    private final AuditService auditService;

    // 1. Service đánh dấu hoàn thành thủ công
    @Transactional
    public void completedWorkshop(Integer eventId, CustomUserDetails userDetails) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        // Kiểm tra trạng thái hiện tại để trả về thông báo cụ thể
        if (event.getWorkshopStatus() == WorkshopStatus.COMPLETED) {
            throw new BadRequestException("Workshop đã hoàn thành rồi, không thể cập nhật thêm!");
        }
        if (event.getWorkshopStatus() == WorkshopStatus.CANCELLED) {
            throw new BadRequestException("Workshop này đã bị hủy, không thể đánh dấu hoàn thành!");
        }
        if (event.getWorkshopStatus() == WorkshopStatus.UPCOMING) {
            throw new BadRequestException("Workshop chưa bắt đầu, bạn không thể kết thúc sớm!");
        }

        // Chỉ khi đang ONGOING mới được phép vượt qua các bước trên
        event.setWorkshopStatus(WorkshopStatus.COMPLETED);
        eventRepository.save(event);
        auditService.saveLog(userDetails.getAccount(), AuditAction.UPDATE_EVENT, AuditEntityType.EVENT, eventId, "Completed workshop of " + event.getEventName());
    }

    // 2. Service hủy workshop
    @Transactional
    public void cancelWorkshop(Integer eventId, CustomUserDetails userDetails) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        // 1. Chỉ chặn nếu đã COMPLETED hoặc đã CANCELLED
        if (event.getWorkshopStatus() == WorkshopStatus.COMPLETED) {
            throw new BadRequestException("Không thể hủy Workshop đã hoàn thành!");
        }
        if (event.getWorkshopStatus() == WorkshopStatus.CANCELLED) {
            throw new BadRequestException("Workshop này đã ở trạng thái hủy từ trước rồi!");
        }

        // 2. Cho phép hủy nếu đang là UPCOMING hoặc ONGOING
        // Nếu status là UPCOMING hoặc ONGOING, code sẽ lọt qua các if chặn ở trên
        event.setWorkshopStatus(WorkshopStatus.CANCELLED);
        eventRepository.save(event);
        auditService.saveLog(userDetails.getAccount(), AuditAction.UPDATE_EVENT, AuditEntityType.EVENT, eventId, "Cancelled workshop of " + event.getEventName());
    }

}
