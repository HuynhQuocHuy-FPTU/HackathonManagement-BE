package com.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class AdminOverviewResponse {
    private AdminMetricsResponse metrics;
    private RoleDistributionResponse roleDistributionResponse;
    private List<AuditLogResponse> recentAuditLogs;

    @Getter
    @Setter
    public static class AdminMetricsResponse{
        private Long highLevelAccounts;
        private Long systemRoles;
        private Long totalLog24h;
        private Long bannedAccounts;
    }
    @Getter
    @Setter
    public static class RoleDistributionResponse{
        private Long studentCount;
        private Long adminCount;
        private Long coordinatorCount;
        private Long expertCount;
        private Long totalUsers;
    }


}
