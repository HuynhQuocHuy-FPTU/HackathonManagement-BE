package com.hackathon.service.submission;

import com.hackathon.dto.submission.FileDTO;
import com.hackathon.dto.submission.SubmissionResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.GithubOAuthService;
import com.hackathon.validator.SubmissionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    private final CloudinaryService cloudinaryService;
    private final SubmissionRepository submissionRepository;
    private final SubmissionValidator submissionValidator;
    private final SubmissionFileRepository submissionFileRepository;
    private final RegistrationRepository registrationRepository;
    private final ParticipantRepository participantRepository;
    private final ExpertAssignRepository expertAssignRepository;
    private final RoundRepository roundRepository;
    private final StudentRepository studentRepository;
    private final GithubOAuthService githubOAuthService;

    @Transactional
    public Submission createSubmission(Integer roundId, String gitHubUrl, CustomUserDetails userDetails, List<MultipartFile> files){
        // 1. Kiểm tra vòng thi tồn tại
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vòng thi!"));
        // 2. Kiểm tra xem có phải là leader không
        Integer studentId = userDetails.getAccount().getStudent().getStudentId();
        Team team = getTeamAsLeader(studentId);
        // 2. Kiểm tra xem leader đã có liên kết tài khoản github chưa và có đúng với tài khoản đã liên kết không
        this.verifyGithubOwnership(gitHubUrl, userDetails);
        // 2. Lấy thông tin Participant (để xác định Team và người nộp)
        Registration approvedRegistration = registrationRepository.findByEventIdAndTeamId(round.getHackathonEvent().getEventId(), team.getTeamId()).orElseThrow(() -> new ResourceNotFoundException("Đội của bạn chưa tham gia vào event"));
        TeamParticipant participant = participantRepository.findTeamParticipantByRegistration_RegistrationIdAndStatus(approvedRegistration.getRegistrationId(), ParticipantStatus.ACTIVE).orElseThrow(() -> new ResourceNotFoundException("Đội của bạn không được phép nộp bài"));


//        if(participant.getCategoryRound() == null){
//            throw new BadRequestException("Đội thi của bạn chưa tham gia vào 1 category cụ thể nào");
//        }

        // 2. Validate (Giờ đây validator sẽ check list files)
        submissionValidator.validateSubmission(round, files, gitHubUrl);

        // 5. Tạo bản ghi Submission (Header)
        Submission submission = new Submission();
        submission.setGithubUrl(gitHubUrl);
        submission.setCreateAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setFinal(false);

        // Thiết lập quan hệ
        submission.setTeam(team);
        submission.setTeamParticipant(participant);
        Submission savedSubmission = submissionRepository.save(submission);

        // 6. Xử lý và lưu danh sách file vào bảng Submission_File
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                FileType type = FileType.fromMimeType(file.getContentType());
                String fileUrl = cloudinaryService.uploadFile(file,type);

                SubmissionFile subFile = SubmissionFile.builder()
                        .submission(savedSubmission)
                        .fileName(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .fileSize(file.getSize())
                        .fileUrl(fileUrl)
                        .uploadedAt(LocalDateTime.now())
                        .build();

                submissionFileRepository.save(subFile);
            }
        }

        return savedSubmission;
    }


    public SubmissionResponse mapToResponse(Submission submission){

        SubmissionResponse response = new SubmissionResponse();
        response.setSubmissionId(submission.getSubmissionId());
        response.setTeamName(submission.getTeamParticipant().getRegistration().getTeam().getTeamName());
        response.setGithubUrl(submission.getGithubUrl());
        response.setStatus(submission.getStatus());
        List<FileDTO> fileDTOList = new ArrayList<>();
        for(SubmissionFile f : submission.getFiles()){
            FileDTO fileDTO = new FileDTO(f.getFileName(), f.getFileUrl());
            fileDTOList.add(fileDTO);
        }
        response.setFileDTOList(fileDTOList);

        return response;
    }


    public List<SubmissionResponse> getSubmissionForJudge(CustomUserDetails userDetails, Integer categoryRoundId) {
        int expertId = userDetails.getAccount().getExpert().getExpertId();

        List<Submission> submissionList = expertAssignRepository.findSubmissionByJudge(categoryRoundId, expertId, List.of(ExpertRole.CORE_JUDGE, ExpertRole.GUEST_JUDGE));

        return submissionList.stream().map(s -> this.mapToResponse(s)).toList();
    }

    public List<SubmissionResponse> getAllSubmission(){
        List<Submission> list = submissionRepository.findAll();

        return list.stream().map(this::mapToResponse).toList();
    }

    public List<SubmissionResponse> getSubmissionForLeader(Integer roundId, CustomUserDetails userDetails){
        Student student = userDetails.getAccount().getStudent();
        List<Submission> list = submissionRepository.findSubmissionForLeader(roundId, student.getStudentId());
        return list.stream().map(this::mapToResponse).toList();
    }
    @Transactional
    public void setNotFinal(Integer roundId, CustomUserDetails userDetails){
        Student student = userDetails.getAccount().getStudent();
        List<Submission> list = submissionRepository.findSubmissionForLeader(roundId, student.getStudentId());
        for(Submission s : list){
            if(s.isFinal()){
                s.setFinal(false);
                submissionRepository.save(s);
            }
        }
    }

    @Transactional
    public void chooseFinalSubmission(Integer submissionId, CustomUserDetails userDetails){
        Integer studentId = userDetails.getAccount().getStudent().getStudentId();
        Team team = getTeamAsLeader(studentId);

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài nộp"));

        // Kiểm tra quyền sở hữu: submission phải thuộc đội của leader đang thao tác
        if (submission.getTeam() == null || submission.getTeam().getTeamId() != team.getTeamId()) {
            throw new BadRequestException("Bạn không có quyền chọn bài nộp này làm bài chính thức");
        }

        // Bỏ đánh dấu final của các submission khác cùng đội trước khi set submission mới
        List<Submission> allSubmissions = submissionRepository.findAll();
        for (Submission s : allSubmissions) {
            boolean sameTeam = s.getTeam() != null && s.getTeam().getTeamId() == team.getTeamId();
            boolean isDifferentSubmission = s.getSubmissionId() != submission.getSubmissionId();
            if (sameTeam && isDifferentSubmission && s.isFinal()) {
                s.setFinal(false);
                submissionRepository.save(s);
            }
        }

        submission.setFinal(true);
        submissionRepository.save(submission);
    }

    private Team getTeamAsLeader(Integer studentId) {
        Student student = studentRepository.findByIdWithTeamMembers(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin sinh viên"));

        return student.getTeamMembers().stream()
                .filter(t -> t.getIsLeader() != null && t.getIsLeader())
                .map(TeamMember::getTeam)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Bạn không phải là leader của đội"));
    }
    /**
     * Xác minh gitHubUrl mà leader nộp thực sự thuộc về (chính là owner của) tài khoản
     * GitHub họ đã liên kết. Chặn trường hợp nộp repo của người khác/team khác.
     */
    private void verifyGithubOwnership(String gitHubUrl, CustomUserDetails userDetails) {
        String linkedGithubUsername = userDetails.getAccount().getGithubUsername();
        if (linkedGithubUsername == null || linkedGithubUsername.isBlank()) {
            throw new BadRequestException("Bạn cần liên kết tài khoản GitHub trước khi nộp bài");
        }

        String repoOwnerLogin = githubOAuthService.fetchRepoOwnerLogin(gitHubUrl);
        if (!linkedGithubUsername.equalsIgnoreCase(repoOwnerLogin)) {
            throw new BadRequestException(
                    "GitHub repo bạn nộp (owner: " + repoOwnerLogin + ") không thuộc về tài khoản GitHub đã liên kết ("
                            + linkedGithubUsername + ")"
            );
        }
    }

}
