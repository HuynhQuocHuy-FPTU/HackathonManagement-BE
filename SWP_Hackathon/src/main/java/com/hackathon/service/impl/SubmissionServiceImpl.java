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
import com.hackathon.service.GithubOAuthService;
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
// Quản lý việc tạo, cập nhật, xem và xác minh bài nộp của đội trong từng vòng thi.
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
    // Tạo bài nộp mới cho đội trong vòng thi và tải các tệp đính kèm lên nơi lưu trữ.
    public Submission createSubmission(Integer roundId, String gitHubUrl, CustomUserDetails userDetails, List<MultipartFile> files){
        // Tìm vòng thi mà đội muốn nộp bài.
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vòng thi!"));
        // Lấy sinh viên đang đăng nhập và xác nhận sinh viên là trưởng nhóm.
        Integer studentId = userDetails.getAccount().getStudent().getStudentId();
        Team team = getTeamAsLeader(studentId);
        // Kiểm tra thời hạn, đường dẫn GitHub và loại tệp theo cấu hình của vòng.
        submissionValidator.validateSubmission(round, files, gitHubUrl);
        // Xác minh kho mã nguồn thuộc tài khoản GitHub mà trưởng nhóm đã liên kết.
        this.verifyGithubOwnership(gitHubUrl, userDetails);
        // Chỉ gọi GitHub lấy mã bản sửa đổi mới nhất khi có đường dẫn kho.
        String latestCommitSha = null;
        if (gitHubUrl != null && !gitHubUrl.isBlank()) {
            latestCommitSha = gitHubService.getLatestCommitSha(gitHubUrl);
        }
        // Tìm đăng ký của đội trong sự kiện và lần tham gia đang hoạt động.
        Registration approvedRegistration = registrationRepository.findByEventIdAndTeamId(round.getHackathonEvent().getEventId(), team.getTeamId()).orElseThrow(() -> new ResourceNotFoundException("Đội của bạn chưa tham gia vào event"));
        TeamParticipant participant = participantRepository.findTeamParticipantByRegistration_RegistrationIdAndStatus(approvedRegistration.getRegistrationId(), ParticipantStatus.ACTIVE).orElseThrow(() -> new ResourceNotFoundException("Đội của bạn không được phép nộp bài"));

        // Đội phải được phân vào một danh mục vòng trước khi có thể nộp bài.
        if(participant.getCategoryRound() == null){
            throw new BadRequestException("Đội thi của bạn chưa tham gia vào 1 category cụ thể nào");
        }

        // Tạo thông tin chính của bài nộp và chụp lại mã bản sửa đổi tại thời điểm nộp.
        Submission submission = new Submission();
        submission.setGithubUrl(gitHubUrl);
        submission.setLatestCommitSha(latestCommitSha);
        submission.setCreateAt(LocalDateTime.now());
        submission.setFinal(false);

        // Liên kết bài nộp với đội và lần tham gia cụ thể trong vòng.
        submission.setTeam(team);
        submission.setTeamParticipant(participant);
        Submission savedSubmission = submissionRepository.save(submission);

        // Chỉ xử lý tệp khi người dùng thực sự gửi ít nhất một tệp đính kèm.
        if (files != null && !files.isEmpty()) {
            // Tải và lưu thông tin từng tệp riêng biệt.
            for (MultipartFile file : files) {
                // Xác định loại tệp từ loại nội dung đã được kiểm tra.
                FileType type = resolveFileType(file);
                // Tải nội dung tệp lên nơi lưu trữ và nhận đường dẫn truy cập.
                String fileUrl = cloudinaryService.uploadFile(file,type);

                SubmissionFile subFile = SubmissionFile.builder()
                        .submission(savedSubmission)
                        .fileName(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .fileSize(file.getSize())
                        .fileUrl(fileUrl)
                        .uploadedAt(LocalDateTime.now())
                        .build();

                // Lưu thông tin tệp và liên kết với bài nộp vừa tạo.
                submissionFileRepository.save(subFile);
            }
        }

        return savedSubmission;
    }

    // Ưu tiên MIME và dùng phần mở rộng làm dự phòng cho tệp nén không có MIME chuẩn.
    private FileType resolveFileType(MultipartFile file) {
        FileType type = FileType.fromMimeType(file.getContentType());
        if (type != null) {
            return type;
        }

        String fileName = file.getOriginalFilename();
        int dotIndex = fileName == null ? -1 : fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex + 1) : null;
        return FileType.fromExtension(extension);
    }

    // Lấy điểm, thứ hạng và trạng thái của bài nộp cuối cùng trong danh mục vòng.
    public ResultSubmissionResponse getResultOfSubmission(CustomUserDetails userDetails, Integer categoryRound) {
        // Xác định sinh viên và đội đang tham gia sự kiện.
        Student student = userDetails.getAccount().getStudent();
        Team team = teamRepository.findCurrentTeamByStudent(student.getStudentId(), TeamStatus.BUSY);
        // Không có đội bận thi đấu nghĩa là sinh viên chưa có kết quả để xem.
        if(team == null){
            throw new BadRequestException("Đội bạn chưa tham gia cuộc thi nào");
        }
        // Xác nhận danh mục vòng được yêu cầu tồn tại.
        CategoryRound cateRound = categoryRoundRepository.findById(categoryRound).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hạng mục - vòng thi"));
        // if(LocalDateTime.now().isBefore(cateRound.getRound().getAppealStartTime())){
        //     return null;
        // }
        // Tìm đúng bài được chọn làm bài chính thức của đội trong danh mục vòng.
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
        response.setGithubUrl(buildGithubUrl(submission));
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

    // Repository chưa có commit thì trả URL gốc, không tạo đường dẫn "/commit/null".
    private String buildGithubUrl(Submission submission) {
        if (submission.getGithubUrl() == null || submission.getGithubUrl().isBlank()) {
            return null;
        }
        if (submission.getLatestCommitSha() == null || submission.getLatestCommitSha().isBlank()) {
            return submission.getGithubUrl();
        }
        return submission.getGithubUrl() + "/commit/" + submission.getLatestCommitSha();
    }

    @Override
    // Lấy các bài nộp thuộc phạm vi giám khảo đang được phân công chấm.
    public List<SubmissionResponse> getSubmissionForJudge(CustomUserDetails userDetails, Integer categoryRoundId) {
        int expertId = userDetails.getAccount().getExpert().getExpertId();

        List<Submission> submissionList = expertAssignRepository.findSubmissionByJudge(categoryRoundId, expertId, List.of(ExpertRole.CORE_JUDGE, ExpertRole.GUEST_JUDGE));

        return submissionList.stream().map(s -> this.mapToResponse(s)).toList();
    }
    @Override
    // Lấy toàn bộ bài nộp trong hệ thống theo thời gian mới nhất trước.
    public List<SubmissionResponse> getAllSubmission(){
        List<Submission> list = submissionRepository.findAllByOrderByCreateAtDesc();
        return list.stream().map(this::mapToResponse).toList();
    }
    @Override
    // Lấy các bài nộp của những đội sinh viên từng tham gia trong một vòng.
    public List<SubmissionResponse> getSubmissionForStudent(Integer roundId, CustomUserDetails userDetails){
        roundRepository.findById(roundId).orElseThrow(() -> new ResourceNotFoundException("Vòng thi không tồn tại"));

        Student student = userDetails.getAccount().getStudent();
        List<Integer> teamIds = teamRepository.findByStudent(student.getStudentId()).stream().map(t -> t.getTeamId()).toList();
        if(teamIds == null){
            throw new BadRequestException("Bạn chưa tham gia vào team nào");
        }

        // Xác nhận ít nhất một đội của sinh viên có tham gia vòng được yêu cầu.
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
    // Bỏ trạng thái chính thức của bài nộp khi vẫn còn trong thời hạn thay đổi.
    public void setNotFinal(Integer submissionId, CustomUserDetails userDetails){
        Integer studentId = userDetails.getAccount().getStudent().getStudentId();
        Team team = getTeamAsLeader(studentId);
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy submission"));

        // Chỉ trưởng nhóm sở hữu bài nộp mới được phép thay đổi.
        if (submission.getTeam() == null
                || submission.getTeam().getTeamId() != team.getTeamId()) {
            throw new BadRequestException(
                    "Bạn không có quyền thay đổi bài nộp này");
        }

        // Không cho thay đổi bài chính thức tại hoặc sau hạn nộp bài.
        Round round = submission.getTeamParticipant()
                .getCategoryRound()
                .getRound();
        if (!LocalDateTime.now().isBefore(round.getSubmissionDeadline())) {
            throw new BadRequestException(
                    "Đã hết thời gian thay đổi bài nộp chính thức");
        }

        // Chỉ bài đang là chính thức mới có thể bị bỏ trạng thái chính thức.
        if (!submission.isFinal()) {
            throw new BadRequestException(
                    "Submission này không phải là bài nộp chính thức");
        }

        // Bỏ cờ chính thức và lưu bài nộp.
        submission.setFinal(false);
        submissionRepository.save(submission);

        // Đồng bộ trạng thái nộp bài của đội về chưa nộp chính thức.
        TeamParticipant participant = submission.getTeamParticipant();
        participant.setSubmissionStatus(SubmissionStatus.NOT_SUBMITTED);
        participantRepository.save(participant);
    }
    @Override
    @Transactional
    // Chọn một bài của đội làm bài nộp chính thức duy nhất trong vòng.
    public void chooseFinalSubmission(Integer submissionId, CustomUserDetails userDetails){
        Integer studentId = userDetails.getAccount().getStudent().getStudentId();
        Team team = getTeamAsLeader(studentId);

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài nộp"));
        // Kiểm tra thời hạn nộp trước khi thay đổi bài chính thức.
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
        // Bỏ cờ chính thức của các bài cũ thuộc cùng lần tham gia.
        List<Submission> previousFinalSubmissions = submissionRepository
                .findByTeamParticipant_IdAndIsFinalTrue(submission.getTeamParticipant().getId());
        previousFinalSubmissions.stream()
                .filter(s -> s.getSubmissionId() != submission.getSubmissionId())
                .forEach(s -> s.setFinal(false));
        submissionRepository.saveAll(previousFinalSubmissions);
        // Đồng bộ trạng thái đội đã có bài nộp chính thức.
        TeamParticipant teamParticipant = submission.getTeamParticipant();
        teamParticipant.setSubmissionStatus(SubmissionStatus.SUBMITTED);
        participantRepository.save(teamParticipant);
        // Đánh dấu bài được chọn là chính thức và lưu thay đổi cuối cùng.
        submission.setFinal(true);
        submissionRepository.save(submission);
    }

    // Tìm đội mà sinh viên hiện giữ vai trò trưởng nhóm.
    private Team getTeamAsLeader(Integer studentId) {
        Student student = studentRepository.findByIdWithTeamMembers(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin sinh viên"));

        return student.getTeamMembers().stream()
                .filter(t -> t.getIsLeader() != null && t.getIsLeader())
                .map(TeamMember::getTeam)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Bạn không phải là leader của đội"));
    }

    // Xác minh chủ sở hữu kho mã nguồn trùng với tài khoản GitHub đã liên kết.
    private void verifyGithubOwnership(String gitHubUrl, CustomUserDetails userDetails) {
        // Không kiểm tra quyền sở hữu khi vòng không yêu cầu đường dẫn GitHub.
        if(gitHubUrl == null || gitHubUrl.isBlank()) return;
        // Tài khoản phải hoàn tất liên kết GitHub trước khi gửi kho mã nguồn.
        String linkedGithubUsername = userDetails.getAccount().getGithubUsername();
        if (linkedGithubUsername == null || linkedGithubUsername.isBlank()) {
            throw new BadRequestException("Bạn cần liên kết tài khoản GitHub trước khi nộp bài");
        }

        // Lấy tên chủ sở hữu thực tế từ GitHub thay vì chỉ tin vào chuỗi đường dẫn.
        String repoOwnerLogin = githubOAuthService.fetchRepoOwnerLogin(gitHubUrl);
        if (!linkedGithubUsername.equalsIgnoreCase(repoOwnerLogin)) {
            throw new BadRequestException(
                    "GitHub repo bạn nộp (owner: " + repoOwnerLogin + ") không thuộc về tài khoản GitHub đã liên kết ("
                            + linkedGithubUsername + ")"
            );
        }
    }

}
