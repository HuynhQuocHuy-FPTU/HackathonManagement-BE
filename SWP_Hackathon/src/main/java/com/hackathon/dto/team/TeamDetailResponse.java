package com.hackathon.dto.team;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TeamDetailResponse {
    private Integer teamId;
    private String teamName;
    private MemberInfo leader;
    private List<MemberInfo> members;
    private LocalDateTime createAt;
    private List<InviteInfo> invitations; // Danh sách Object chứa cả email và status

    @Getter
    @Setter
    @AllArgsConstructor
    public static class MemberInfo {
        private String studentCode;
        private String fullName;
        private String email;
        private String major;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class InviteInfo {
        private String email;
        private String status;
    }
}