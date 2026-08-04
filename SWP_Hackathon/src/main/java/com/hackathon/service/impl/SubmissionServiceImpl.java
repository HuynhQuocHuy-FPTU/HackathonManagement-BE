package com.hackathon.service.impl;

import com.hackathon.dto.submission.FileDTO;
import com.hackathon.dto.submission.ResultSubmissionResponse;
import com.hackathon.dto.submission.SubmissionResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.submission.CloudinaryService;
import com.hackathon.service.submission.GitHubService;
import com.hackathon.service.submission.SubmissionService;
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
public class SubmissionServiceImpl implements SubmissionService {

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
    private final TeamRepository teamRepository;
    private final GitHubService gitHubService;
    private final CategoryRoundRepository categoryRoundRepository;

    @Override
    @Transactional
    public Submission createSubmission(Integer roundId, String gitHubUrl, CustomUserDetails userDetails, List<MultipartFile> files){
        // 1. Kiểm tra vòng thi tồn tại
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vòng thi!"));
        // 2. Kiểm tra xem có phải là leader không
        Integer studentId = userDetails.getAccount().getStudent().getStudentId();
        Team team = getTeamAsLeader(studentId);
        // 2. Validate (Giờ đây validator sẽ check list files)
        submissionValidator.validateSubmission(round, files, gitHubUrl);
        // 2. Kiểm tra xem leader đã có liên kết tài khoản github chưa và có đúng với tài khoản đã liên kết không
        this.verifyGithubOwnership(gitHubUrl, userDetails);
        String latestCommitSha = null;
        if (gitHubUrl != null && !gitHubUrl.isBlank()) {
            latestCommitSha = gitHubService.getLatestCommitSha(gitHubUrl);
        }
        // 2. Lấy thông tin Participant (để xác định Team và người nộp)
        Registration approvedRegistration = registrationRepository.findByEventIdAndTeamId(round.getHackathonEvent().getEventId(), team.getTeamId()).orElseThrow(() -> new ResourceNotFoundException("Đội của bạn chưa tham gia vào event"));
        TeamParticipant participant = participantRepository.findTeamParticipantByRegistration_RegistrationIdAndStatus(approvedRegistration.getRegistrationId(), ParticipantStatus.ACTIVE).orElseThrow(() -> new ResourceNotFoundException("Đội của bạn không được phép nộp bài"));

        if(participant.getCategoryRound() == null){
            throw new BadRequestException("Đội thi của bạn chưa tham gia vào 1 category cụ thể nào");
        }

        // 5. Tạo bản ghi Submission (Header)
        Submission submission = new Submission();
        submission.setGithubUrl(gitHubUrl);
        submission.setLatestCommitSha(latestCommitSha);
        submission.setCreateAt(LocalDateTime.now());
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

    public ResultSubmissionResponse getResultOfSubmission(CustomUserDetails userDetails, Integer categoryRound) {
        Student student = userDetails.getAccount().getStudent();
        Team team = teamRepository.findCurrentTeamByStudent(student.getStudentId(), TeamStatus.BUSY);
        if(team == null){
            throw new BadRequestException("Đội bạn chưa tham gia cuộc thi nào");
        }
        CategoryRound cateRound = categoryRoundRepository.findById(categoryRound).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hạng mục - vòng thi"));
        if(LocalDateTime.now().isBefore(cateRound.getRound().getAppealStartTime())){
            return null;
        }
        Submission submission = submissionRepository.findFinalSubmission(categoryRound, team.getTeamId());
        if(submission == null){
            throw new BadRequestException("Bạn chưa có bài nộp cuối cùng hoặc chưa có điểm");
        }
        return ResultSubmissionResponse.builder()
                .submissionId(submission.getSubmissionId())
                .totalScore(submission.getTeamParticipant().getTotalScore())
                .submissionStatus(submission.getTeamParticipant().getSubmissionStatus())
                .rank(submission.getTeamParticipant().getRank())
                .build();
    }


    public SubmissionResponse mapToResponse(Submission submission){

        SubmissionResponse response = new SubmissionResponse();
        response.setSubmissionId(submission.getSubmissionId());
        response.setTeamName(submission.getTeamParticipant().getRegistration().getTeam().getTeamName());
        response.setGithubUrl(submission.getGithubUrl() + "/commit/" + submission.getLatestCommitSha());
        List<FileDTO> fileDTOList = new ArrayList<>();
        for(SubmissionFile f : submission.getFiles()){
            FileDTO fileDTO = new FileDTO(f.getFileName(), f.getFileUrl());
            fileDTOList.add(fileDTO);
        }
        response.setFileDTOList(fileDTOList);
        response.setCreateAt(submission.getCreateAt());
        response.setFinal(submission.isFinal());
        response.setStatus(submission.getTeamParticipant().getSubmissionStatus());
        return response;
    }

    @Override
    public List<SubmissionResponse> getSubmissionForJudge(CustomUserDetails userDetails, Integer categoryRoundId) {
        int expertId = userDetails.getAccount().getExpert().getExpertId();

        List<Submission> submissionList = expertAssignRepository.findSubmissionByJudge(categoryRoundId, expertId, List.of(ExpertRole.CORE_JUDGE, ExpertRole.GUEST_JUDGE));

        return submissionList.stream().map(s -> this.mapToResponse(s)).toList();
    }
    @Override
    public List<SubmissionResponse> getAllSubmission(){
        List<Submission> list = submissionRepository.findAllByOrderByCreateAtDesc();
        return list.stream().map(this::mapToResponse).toList();
    }
    @Override
    public List<SubmissionResponse> getSubmissionForStudent(Integer roundId, CustomUserDetails userDetails){
        roundRepository.findById(roundId).orElseThrow(() -> new ResourceNotFoundException("Vòng thi không tồn tại"));

        Student student = userDetails.getAccount().getStudent();
        List<Integer> teamIds = teamRepository.findByStudent(student.getStudentId()).stream().map(t -> t.getTeamId()).toList();
        if(teamIds == null){
            throw new BadRequestException("Bạn chưa tham gia vào team nào");
        }

        boolean isParticipating = participantRepository.existsByRegistration_Team_TeamIdInAndCategoryRound_Round_RoundId(teamIds, roundId);

        if (!isParticipating) {
            throw new BadRequestException("Bạn không tham gia vòng thi này");
        }
        List<Submission> list = submissionRepository.findSubmissionForStudent(roundId, teamIds);

        if(list.isEmpty()){
            throw new BadRequestException("Team bạn tham gia chưa nộp bài nào cho vòng thi này");
        }
        return list.stream().map(this::mapToResponse).toList();
    }
    @Override
    @Transactional
    public void setNotFinal(Integer submissionId, CustomUserDetails userDetails){
        Integer studentId = userDetails.getAccount().getStudent().getStudentId();
        Team team = getTeamAsLeader(studentId);
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy submission"));

        if (submission.getTeam() == null
                || submission.getTeam().getTeamId() != team.getTeamId()) {
            throw new BadRequestException(
                    "Bạn không có quyền thay đổi bài nộp này");
        }

        Round round = submission.getTeamParticipant()
                .getCategoryRound()
                .getRound();
        if (!LocalDateTime.now().isBefore(round.getSubmissionDeadline())) {
            throw new BadRequestException(
                    "Đã hết thời gian thay đổi bài nộp chính thức");
        }

        if (!submission.isFinal()) {
            throw new BadRequestException(
                    "Submission này không phải là bài nộp chính thức");
        }

        submission.setFinal(false);
        submissionRepository.save(submission);

        TeamParticipant participant = submission.getTeamParticipant();
        participant.setSubmissionStatus(SubmissionStatus.NOT_SUBMITTED);
        participantRepository.save(participant);
    }
    @Override
    @Transactional
    public void chooseFinalSubmission(Integer submissionId, CustomUserDetails userDetails){
        Integer studentId = userDetails.getAccount().getStudent().getStudentId();
        Team team = getTeamAsLeader(studentId);

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài nộp"));
        if(!LocalDateTime.now().isBefore(submission.getTeamParticipant().getCategoryRound().getRound().getSubmissionDeadline())){
            throw new BadRequestException("Đã hết thời gian nộp bài");
        }
        // Kiểm tra quyền sở hữu: submission phải thuộc đội của leader đang thao tác
        if (submission.getTeam() == null
                || !java.util.Objects.equals(submission.getTeam().getTeamId(), team.getTeamId())) {
            throw new BadRequestException("Bạn không có quyền chọn bài nộp này làm bài chính thức");
        }

        // Mỗi TeamParticipant (team trong một CategoryRound) chỉ có một bài chính thức.
        // Không thay đổi bài chính thức của team ở vòng hoặc hạng mục khác.
        List<Submission> previousFinalSubmissions = submissionRepository
                .findByTeamParticipant_IdAndIsFinalTrue(submission.getTeamParticipant().getId());
        previousFinalSubmissions.stream()
                .filter(s -> s.getSubmissionId() != submission.getSubmissionId())
                .forEach(s -> s.setFinal(false));
        submissionRepository.saveAll(previousFinalSubmissions);
        TeamParticipant teamParticipant = submission.getTeamParticipant();
        teamParticipant.setSubmissionStatus(SubmissionStatus.SUBMITTED);
        participantRepository.save(teamParticipant);
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

    private void verifyGithubOwnership(String gitHubUrl, CustomUserDetails userDetails) {
        if(gitHubUrl == null || gitHubUrl.isBlank()) return;
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
