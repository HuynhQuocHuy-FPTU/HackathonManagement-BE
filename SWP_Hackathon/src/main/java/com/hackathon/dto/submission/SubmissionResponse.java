package com.hackathon.dto.submission;

import com.hackathon.entity.Submission;
import com.hackathon.entity.enums.SubmissionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SubmissionResponse {

    private Integer submissionId;
    private String teamName;
    private String githubUrl;
    private List<FileDTO> fileDTOList;
    private SubmissionStatus status;
    private LocalDateTime createAt;
    private boolean isFinal;
}
