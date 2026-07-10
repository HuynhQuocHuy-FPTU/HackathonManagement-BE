package com.hackathon.service;

import com.hackathon.dto.AdminOverviewResponse;
import com.hackathon.dto.AuditLogResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final StudentRepository studentRepository;
    private final ExpertRepository expertRepository;
    private final ObjectMapper objectMapper;
    private final AccountRepository accountRepository;

    public void saveLog(Account acc, AuditAction action, AuditEntityType entityType, Integer entityId, String description, String data) {
        String actorName = "N/A";
        if (acc != null && acc.getRole() != null) {
            int accountId = acc.getAccountId();
            switch (acc.getRole().name()) {
                case "EVENTCOORDINATOR":
                    actorName = eventCoordinatorRepository.findByAccount_AccountId(accountId)
                            .map(EventCoordinator::getCoordinatorName)
                            .orElse("N/A");
                    break;
                case "STUDENT":
                    actorName = studentRepository.findByAccount_AccountId(accountId)
                            .map(Student::getStudentName)
                            .orElse("N/A");
                    break;
                case "EXPERT":
                    actorName = expertRepository.findByAccount_AccountId(accountId)
                            .map(Expert::getExpertName)
                            .orElse("N/A");
                    break;
                default:
                    actorName = "ADMIN";
                    break;
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
        auditLogRepository.save(auditLog);
    }

    public void saveLog(Account acc, AuditAction action, AuditEntityType entityType, Integer entityId, String description) {
//        AuditLog auditLog = new AuditLog();
//        auditLog.setAction(action);
//        auditLog.setEntityType(entityType);
//        auditLog.setEntityId(entityId);
//        auditLog.setAccount(acc);
//        auditLog.setDescription(description);
//        auditLogRepository.save(auditLog);
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
//        if (log.getData() != null && !log.getData().trim().isEmpty()) {
//            try {
//                res.setData(objectMapper.readTree(log.getData()));
//            } catch (Exception e) {
//                res.setData(log.getData());
//            }
//        }
        return res;
    }

    public Page<AuditLogResponse> getAllAuditLog(Pageable pageable) {
        Page<AuditLog> list = auditLogRepository.findAll(pageable);
        return list.map(this::toResponse);
    }

    public AdminOverviewResponse getOverviewForAdmin() {

        long totalRoles = AccountRole.values().length;
        long highLevelAccounts = accountRepository.countByRole(AccountRole.EXPERT)
                + accountRepository.countByRole(AccountRole.EVENTCOORDINATOR);
        LocalDateTime time = LocalDateTime.now().minusHours(24);
        long totalLogs24h = auditLogRepository.countTotalLogs24h(time);
        long bannedAccounts = accountRepository.countByStatus(AccountStatus.BANNED)
                + accountRepository.countByStatus(AccountStatus.INACTIVE);

        // Metrics
        AdminOverviewResponse.AdminMetricsResponse metrics = new AdminOverviewResponse.AdminMetricsResponse();
        metrics.setSystemRoles(totalRoles);
        metrics.setTotalLog24h(totalLogs24h);
        metrics.setHighLevelAccounts(highLevelAccounts);
        metrics.setBannedAccounts(bannedAccounts);
        //Role distribution
        long studentCount = accountRepository.countByRole(AccountRole.STUDENT);
        long adminCount = accountRepository.countByRole(AccountRole.ADMIN);
        long coordinatorCount = accountRepository.countByRole(AccountRole.EVENTCOORDINATOR);
        long expertCount = accountRepository.countByRole(AccountRole.EXPERT);
        long totalUser = studentCount + adminCount + coordinatorCount + expertCount;
        AdminOverviewResponse.RoleDistributionResponse distribution = new AdminOverviewResponse.RoleDistributionResponse();
        distribution.setStudentCount(studentCount);
        distribution.setAdminCount(adminCount);
        distribution.setCoordinatorCount(coordinatorCount);
        distribution.setExpertCount(expertCount);
        distribution.setTotalUsers(totalUser);
        //RecentAuditLogs
        List<AuditLog> list = auditLogRepository.findTop10ByOrderByCreatedAtDesc();
        List<AuditLogResponse> recentLogs = new ArrayList<>();
        for (AuditLog log : list) {
            AuditLogResponse res = new AuditLogResponse();
            res.setId(log.getId());
            res.setAccountId(log.getAccount().getAccountId());
            res.setAction(log.getAction().name());
            res.setRole(log.getAccount().getRole());
            res.setCreatedAt(log.getCreatedAt());
            res.setActorName(log.getActorName());
            recentLogs.add(res);
        }
        return new AdminOverviewResponse(
                metrics,
                distribution,
                recentLogs
        );


    }

}
