package com.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class StudentDTO {
    private Integer studentId;
    private String address;
    private String major;
    private LocalDateTime startDate;
    private String status;
    private String studentCode;
    private String studentName;
    private Integer accountId;
    private String organizationId;


}
