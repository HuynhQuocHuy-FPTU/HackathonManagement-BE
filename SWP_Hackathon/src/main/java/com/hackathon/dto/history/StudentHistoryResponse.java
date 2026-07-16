package com.hackathon.dto.history;

import com.hackathon.dto.round.RoundStatusDTO;
import com.hackathon.entity.enums.ExpertRole;
import com.hackathon.entity.enums.RegistrationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentHistoryResponse {
    private String studentName;
    private String universityName;
    private LocalDateTime creatAt;    // ngày tạo tài khoản
    private List<StudentHistory> list;



    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StudentHistory {
        private String teamName;
        private Integer eventId;
        private String eventName;
        private RegistrationStatus status;     // trang thai dk event
        private LocalDateTime registrationDate;// ngay dk event
        private boolean isLeader;
        private Integer ranking;
        private List<RoundStatusDTO> listRounds;
        private String reward;


    }

}
