package com.hackathon.dto.team;

import com.hackathon.entity.enums.NotificationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeamResponse {
    private Integer teamId;
    private String teamName;
    private MemberInfo leader; // Thông tin riêng của Leader
    private List<MemberInfo> members;// Danh sách các thành viên còn lại
    private LocalDateTime createAt;
    private List<String> invitedEmails;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class MemberInfo {
        private String studentCode;
        private String fullName;
        private String email;
    }

}
