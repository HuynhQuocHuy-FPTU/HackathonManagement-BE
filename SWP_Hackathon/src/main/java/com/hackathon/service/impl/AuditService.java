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
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final StudentRepository studentRepository;
    private final ExpertRepository expertRepository;
    private final ObjectMapper objectMapper;
    private final AccountRepository accountRepository;

    public AuditLog saveLog(Account acc, AuditAction action, AuditEntityType entityType, Integer entityId, String description, String data) {
        String actorName = "SYSTEM";
        if (acc!=null && acc.getRole() != null) {

            int accountId = acc.getAccountId();
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


        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAccount(acc);
        auditLog.setDescription(description);
        auditLog.setData(data);
        auditLog.setActorName(actorName);
        return auditLogRepository.save(auditLog);
    }

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

    public Page<AuditLogResponse> getAllAuditLog(Pageable pageable) {
        Page<AuditLog> list = auditLogRepository.findAll(pageable);
        return list.map(this::toResponse);
    }


}
