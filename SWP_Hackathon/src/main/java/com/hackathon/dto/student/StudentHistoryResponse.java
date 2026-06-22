package com.hackathon.dto.student;

import com.hackathon.entity.enums.RegistrationStatus;
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
public class StudentHistoryResponse {
    private String studentName;
    private String universityName;
    private LocalDateTime creatAt;    // ngày tạo tài khoản
    private List<StudentHistory> list;



    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StudentHistory {
        private String teamName;
        private String eventName;
        private RegistrationStatus status;// trang thai dk event
        private LocalDateTime registrationDate;// ngay dk event
        private boolean isLeader;
        private Integer ranking;
//        private String reward;

    }

}
