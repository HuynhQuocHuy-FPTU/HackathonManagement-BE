package com.hackathon.service.impl;

import com.hackathon.dto.AuditLogResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
// Ghi và truy xuất lịch sử các thao tác quan trọng được thực hiện trong hệ thống.
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final StudentRepository studentRepository;
    private final ExpertRepository expertRepository;
    private final ObjectMapper objectMapper;

    // Tạo lịch sử thao tác kèm dữ liệu chi tiết và xác định tên người thực hiện theo vai trò.
    public AuditLog saveLog(Account acc, AuditAction action, AuditEntityType entityType, Integer entityId, String description, String data) {
        // Mặc định thao tác được xem là do hệ thống thực hiện khi không có tài khoản cụ thể.
        String actorName = "SYSTEM";
        // Chỉ tìm tên hiển thị khi tài khoản và vai trò đều tồn tại.
        if (acc!=null && acc.getRole() != null) {

            // Lấy mã tài khoản dùng để tra cứu hồ sơ riêng theo vai trò.
            int accountId = acc.getAccountId();
            // Chọn đúng nguồn dữ liệu chứa tên của người thực hiện.
            switch (acc.getRole()) {
                case EVENTCOORDINATOR:
                    actorName = eventCoordinatorRepository.findByAccount_AccountId(accountId)
                            .map(EventCoordinator::getCoordinatorName)
                            .orElse("N/A");
                    break;
                case STUDENT:
                    actorName = studentRepository.findByAccount_AccountId(accountId)
                            .map(Student::getStudentName)
                            .orElse("N/A");
                    break;
                case EXPERT:
                    actorName = expertRepository.findByAccount_AccountId(accountId)
                            .map(Expert::getExpertName)
                            .orElse("N/A");
                    break;
                default:
                    actorName = "SYSTEM";
            }
        }


        // Tạo bản ghi lịch sử sau khi đã xác định đầy đủ người thực hiện.
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAccount(acc);
        auditLog.setDescription(description);
        auditLog.setData(data);
        auditLog.setActorName(actorName);
        // Lưu và trả về bản ghi để các lịch sử chuyên biệt có thể liên kết tiếp.
        return auditLogRepository.save(auditLog);
    }

    // Ghi lịch sử không kèm dữ liệu mở rộng bằng cách dùng chung hàm lưu đầy đủ.
    public void saveLog(Account acc, AuditAction action, AuditEntityType entityType, Integer entityId, String description) {
        this.saveLog(acc, action, entityType, entityId, description, null);

    }

    public AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse res = new AuditLogResponse();

        res.setId(log.getId());
        res.setAccountId(log.getAccount().getAccountId());
        res.setAction(log.getAction().name());
        res.setEntityType(log.getEntityType().name());
        res.setEntityId(log.getEntityId());
        res.setRole(log.getAccount().getRole());
        res.setMessage(log.getDescription());
        res.setCreatedAt(log.getCreatedAt());
        res.setActorName(log.getActorName());
        if (log.getData() != null && !log.getData().trim().isEmpty()) {
            try {
                res.setData(objectMapper.readTree(log.getData()));
            } catch (Exception e) {
                res.setData(log.getData());
            }
        }
        return res;
    }

    // Lấy lịch sử theo từng trang để tránh tải toàn bộ dữ liệu cùng lúc.
    public Page<AuditLogResponse> getAllAuditLog(Pageable pageable) {
        // Truy vấn đúng trang và thông tin sắp xếp được truyền vào.
        Page<AuditLog> list = auditLogRepository.findAll(pageable);
        return list.map(this::toResponse);
    }


}
