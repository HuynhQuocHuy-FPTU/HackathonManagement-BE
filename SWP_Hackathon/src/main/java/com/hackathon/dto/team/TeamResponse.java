package com.hackathon.dto.team;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class TeamResponse {
    private Integer teamId;
    private String teamName;
    private Integer eventId;
    private String title;
    private MemberInfo leader; // Thông tin riêng của Leader
    private List<MemberInfo> members;// Danh sách các thành viên còn lại
    private LocalDateTime createAt;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class MemberInfo {
        private Integer userId;
        private String studentCode;
        private String fullName;
        private String email;
    }
}
