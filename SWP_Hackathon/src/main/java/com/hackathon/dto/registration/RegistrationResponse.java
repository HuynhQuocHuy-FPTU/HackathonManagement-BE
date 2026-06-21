package com.hackathon.dto.registration;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistrationResponse {
    private Integer registrationId;
    private String eventName;
    private Integer teamId;
    private String teamName;
    private LocalDateTime registrationDate;
    private Integer teamSize;
    private RegistrationResponse.MemberInfo leader; // Thông tin riêng của Leader
    private List<RegistrationResponse.MemberInfo> members;// Danh sách các thành viên còn lại
    @Getter
    @Setter
    @AllArgsConstructor
    @Builder
    public static class MemberInfo {
        private String studentCode;
        private String fullName;
        private String major;
        private String email;
    }


}