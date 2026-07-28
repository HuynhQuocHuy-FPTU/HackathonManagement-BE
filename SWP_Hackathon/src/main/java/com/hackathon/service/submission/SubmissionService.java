package com.hackathon.service.submission;

import com.hackathon.dto.submission.ResultSubmissionResponse;
import com.hackathon.dto.submission.SubmissionResponse;
import com.hackathon.entity.Submission;
import com.hackathon.security.CustomUserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SubmissionService {
    Submission createSubmission(Integer roundId, String gitHubUrl, CustomUserDetails userDetails, List<MultipartFile> files);

    public ResultSubmissionResponse getResultOfSubmission(CustomUserDetails userDetails, Integer categoryRound);

    public List<SubmissionResponse> getSubmissionForJudge(CustomUserDetails userDetails, Integer categoryRoundId);

    public List<SubmissionResponse> getAllSubmission();

    public List<SubmissionResponse> getSubmissionForStudent(Integer roundId, CustomUserDetails userDetails);

    public void setNotFinal(Integer submissionId, CustomUserDetails userDetails);

    public void chooseFinalSubmission(Integer submissionId, CustomUserDetails userDetails);


}
