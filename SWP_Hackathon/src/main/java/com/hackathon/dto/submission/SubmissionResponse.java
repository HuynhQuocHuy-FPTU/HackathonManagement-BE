package com.hackathon.dto.submission;

import com.hackathon.entity.Submission;
import com.hackathon.entity.enums.SubmissionStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class SubmissionResponse {

    private Integer submissionId;
    private String teamName;
    private String githubUrl;
    private List<FileDTO> fileDTOList;
    private SubmissionStatus status;
}
