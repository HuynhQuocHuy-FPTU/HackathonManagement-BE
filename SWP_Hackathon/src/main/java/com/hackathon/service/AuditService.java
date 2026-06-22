package com.hackathon.service;

import com.hackathon.dto.AuditLogResponse;
import com.hackathon.entity.Account;
import com.hackathon.entity.AuditLog;
import com.hackathon.entity.enums.AuditAction;
import com.hackathon.entity.enums.AuditEntityType;
import com.hackathon.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void saveLog(Account acc, AuditAction action, AuditEntityType entityType, Integer entityId, String description){
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAccount(acc);
        auditLog.setDescription(description);

        auditLogRepository.save(auditLog);
    }
    public AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse res = new AuditLogResponse();

        res.setId(log.getId());
        res.setAction(log.getAction().name());
        res.setEntityType(log.getEntityType().name());
        res.setEntityId(log.getEntityId());
        res.setRole(log.getAccount().getRole());
        res.setMessage(log.getDescription());
        res.setCreatedAt(log.getCreatedAt());
        return res;
    }

    public List<AuditLogResponse> getAllAuditLog(){
        List<AuditLog> list = auditLogRepository.findAll();

        return list.stream().map(this::toResponse).toList();
    }
}
