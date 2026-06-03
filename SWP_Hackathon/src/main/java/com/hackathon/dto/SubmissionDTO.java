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
public class SubmissionDTO {
    private Integer submissionId;
    private LocalDateTime createDate;
    private String description;
    private String fileUrl;
    private String githubUrl;
    private String status;
    private Integer categoryRoundId;
    private Integer teamId;
}