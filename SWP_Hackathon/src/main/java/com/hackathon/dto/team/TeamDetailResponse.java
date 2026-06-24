package com.hackathon.dto.team;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hackathon.entity.enums.TeamStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeamDetailResponse {
    private Integer teamId;
    private String teamName;
    private MemberInfo leader;
    private List<MemberInfo> members;// chứa thông tin thành viên
    private LocalDateTime createAt;
    private List<InviteInfo> invitations;
    private int sizeTeam;
    private String status;
    private String categoryName;
    private Integer expertId;
    private String roundName;


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MemberInfo {
        private String studentCode;
        private String fullName;
        private String email;
        private String major;
        private boolean isLeader;
        private String university;
        private String avatarUrl;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InviteInfo {
        private String email;
        private String status;
    }
}