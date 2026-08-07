package com.hackathon.service.impl;

import com.hackathon.dto.participant.*;
import com.hackathon.dto.round.RoundStatusDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.NotificationService;
import com.hackathon.service.ParticipantService;
import com.hackathon.validator.DisqualifyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
// Quản lý đội tham gia từng vòng, thông tin phân công và việc loại đội khỏi cuộc thi.
public class ParticipantServiceImpl implements ParticipantService {
    private final ExpertRepository expertRepository;
    private final ExpertAssignRepository expertAssignRepository;
    private final ParticipantRepository participantRepository;
    private final TeamRepository teamRepository;
    private final DisqualifyValidator disqualifyValidator;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final HackathonEventRepository hackathonEventRepository;
    private final RoundRepository roundRepository;
    private final RoundAdvancementServiceImpl roundAdvancementServiceImpl;
    private final RegistrationRepository registrationRepository;
    private final CategoryRoundRepository categoryRoundRepository;
    private final EvaluationRepository evaluationRepository;
    private final StudentRepository studentRepository;



    // Lấy các nhóm thí sinh thuộc phạm vi chuyên gia đang được phân công.
    public List<ExpertAssignedGroupDTO> getAssignParticipants(Integer eventId, CustomUserDetails userDetails) {

        // Lấy tài khoản từ phiên đăng nhập để xác định chuyên gia đang yêu cầu dữ liệu.
        Account account = userDetails.getAccount();

        // Tìm hồ sơ chuyên gia gắn với mã tài khoản hiện tại.
        Expert expert = expertRepository
                .findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() ->
                        new BadRequestException(
                                "Không tìm thấy expert ứng với account " + account.getEmail()));

        // Lấy toàn bộ phân công của chuyên gia trong đúng sự kiện được yêu cầu.
        List<ExpertAssign> assigns = expertAssignRepository.findExpertAssignments(expert.getExpertId(), eventId);

        // Không có phân công đồng nghĩa chuyên gia không được phép xem đội trong sự kiện này.
        if (assigns.isEmpty()) {
            throw new BadRequestException("Expert không được phân công trong event này");
        }

        return assigns.stream().map(this::buildGroup).toList();
    }

    // Loại đội khỏi sự kiện, lưu lý do và xử lý đội thay thế nếu cần.
    public void disqualifyTeam(Integer eventId, Integer teamId, String reason) {
        // Lấy người đang xác thực từ vùng lưu trữ bảo mật vì hàm không nhận trực tiếp thông tin đăng nhập.
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Tài khoản này được dùng làm người thực hiện trong lịch sử thao tác.
        Account account = userDetails.getAccount();
        // Lấy toàn bộ lần tham gia các vòng của đúng đội trong đúng sự kiện.
        List<TeamParticipant> teamParticipants = participantRepository.findParticipantByRegistration_Team_TeamIdAndRegistration_HackathonEvent_EventId(teamId, eventId);
        // Tìm sự kiện để kiểm tra tồn tại và lấy tên dùng trong thông báo.
        HackathonEvent event = hackathonEventRepository.findById(eventId).orElseThrow(() -> new BadRequestException("Không tìm thấy event"));

        // Xác nhận đội thật sự thuộc sự kiện và đăng ký của đội đã được duyệt.
        teamParticipants = disqualifyValidator.validateTeamBelongsToEventAndApproved(teamParticipants, teamId, eventId);

        // Lưu lại teamParticipant PASSED ở round gần nhất mà team đã pass
        // Tìm lần vượt vòng gần nhất để biết đội đã chiếm một suất ở vòng kế tiếp hay chưa.
        TeamParticipant mostRecentlyPassed = teamParticipants.stream()
                .filter(p -> p.getStatus() == ParticipantStatus.PASSED)
                .max(Comparator.comparing(p -> p.getCategoryRound().getRound().getOrderIndex()))
                .orElse(null);

        // Đánh dấu tất cả lần tham gia của đội là bị loại và dùng chung lý do được cung cấp.
        for (TeamParticipant teamParticipant : teamParticipants) {
            // Chuyển trạng thái của lần tham gia hiện tại sang bị loại.
            teamParticipant.setStatus(ParticipantStatus.DISQUALIFIED);
            // Lưu lý do cụ thể để hiển thị lại cho đội và ban tổ chức.
            teamParticipant.setDisqualificationReason(reason);
        }
        // Lưu đồng loạt để trạng thái của đội nhất quán ở tất cả các vòng.
        participantRepository.saveAll(teamParticipants);

        // Lấy đội từ đăng ký đã được bộ kiểm tra xác nhận hợp lệ.
        Team team = teamParticipants.get(0).getRegistration().getTeam();
        // Đưa đội về trạng thái nháp vì đội không còn thi đấu trong sự kiện này.
        team.setStatus(TeamStatus.DRAFT);
        // Lưu trạng thái mới của đội.
        teamRepository.save(team);

        // 5. Nếu team từng PASSED 1 vòng nào đó, thử đôn team thay thế — CHỈ KHI round kế tiếp CHƯA bắt đầu.
        if (mostRecentlyPassed != null) {
            // Tìm vòng ngay sau vòng gần nhất mà đội đã vượt qua.
            Round nextRound = roundRepository
                    .findRoundByHackathonEvent_EventIdAndOrderIndex(
                            eventId, mostRecentlyPassed.getCategoryRound().getRound().getOrderIndex() + 1)
                    .orElse(null);

            // Chỉ được đôn đội thay thế khi vòng kế tiếp tồn tại và chưa bắt đầu.
            boolean nextRoundNotStartedYet = nextRound != null
                    && (nextRound.getStartTime() == null || LocalDateTime.now().isBefore(nextRound.getStartTime()));

            if (nextRoundNotStartedYet) {
                // Chọn đội có thứ hạng kế tiếp để thay thế suất của đội vừa bị loại.
                roundAdvancementServiceImpl.disqualifyRetroactively(mostRecentlyPassed, nextRound);
            } else {
                log.info("Team {} từng PASSED nhưng round kế tiếp đã bắt đầu hoặc không tồn tại, " + "không đôn team thay thế.", team.getTeamName());
            }
        }

        // Tìm tài khoản trưởng nhóm để gửi thông báo chính thức về quyết định loại đội.
        Account accountLeader = team.getTeamMembers()
                .stream()
                .filter(TeamMember::getIsLeader)
                .map(TeamMember::getStudent)
                .map(Student::getAccount)
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException("Không tìm thấy trưởng nhóm"));
        // Ghi lại người thực hiện, đối tượng bị tác động và tên đội trong lịch sử hệ thống.
        auditService.saveLog(account,
                AuditAction.DISQUALIFY_TEAM,
                AuditEntityType.PARTICIPANT,
                teamParticipants.get(0).getId(),
                team.getTeamName());

        // Gửi thông báo kèm tên sự kiện và lý do loại đến trưởng nhóm.
        notificationService.notifyDisqualifyTeam(account, accountLeader, team.getTeamName(), event.getEventName(), reason);
    }

    private ParticipantResponseDTO mapToResponse(TeamParticipant teamParticipant) {
        if (teamParticipant == null) {
            return null;
        }
        String teamName = teamParticipant.getRegistration().getTeam().getTeamName();

        return ParticipantResponseDTO.builder()
                .participantId(teamParticipant.getId())
                .teamName(teamName)
                .totalScore(teamParticipant.getTotalScore())
                .rank(teamParticipant.getRank())
                .status(teamParticipant.getStatus())
                .build();
    }

    private ExpertAssignedGroupDTO buildGroup(ExpertAssign expertAssign) {
        CategoryRound categoryRound = expertAssign.getCategoryRound();

        List<TeamParticipant> teamParticipants = participantRepository.findParticipantByCategoryRound_CategoryRoundId(categoryRound.getCategoryRoundId());

        List<ParticipantResponseDTO> participantResponseDTOS = teamParticipants.stream().map(this::mapToResponse).toList();

        return ExpertAssignedGroupDTO.builder()
                .categoryRoundId(categoryRound.getCategoryRoundId())
                .categoryId(categoryRound.getCategory().getCategoryId())
                .categoryName(categoryRound.getCategory().getCategoryName())
                .roundId(categoryRound.getRound().getRoundId())
                .roundName(categoryRound.getRound().getRoundName())
                .role(expertAssign.getRole())
                .participants(participantResponseDTOS)
                .build();
    }

    // Tạo bản ghi tham gia vòng đầu tiên từ đăng ký sự kiện đã được duyệt.
    public TeamParticipant saveParticipant(Registration registration) {
        // Tạo lần tham gia mới từ đăng ký sự kiện vừa được duyệt.
        TeamParticipant teamParticipant = new TeamParticipant();
        // Liên kết lần tham gia với đăng ký nguồn.
        teamParticipant.setRegistration(registration);
        // Chưa gán danh mục vòng cho đến khi có kết quả phân loại hoặc bốc thăm.
        teamParticipant.setCategoryRound(null);
        // Đội chưa có bài nộp tại thời điểm vừa được tạo.
        teamParticipant.setSubmissionStatus(SubmissionStatus.NOT_SUBMITTED);
        // Kích hoạt lần tham gia để đội có thể tiếp tục các bước thi đấu.
        teamParticipant.setStatus(ParticipantStatus.ACTIVE);

        // Lưu và trả về lần tham gia vừa được tạo.
        return participantRepository.save(teamParticipant);
    }

    @Override
    // Xác định đội và vòng thi hiện tại của sinh viên đang đăng nhập.
    public CurrentParticipantDTO getCurrentParticipant(CustomUserDetails userDetails) {
        // Tải lại sinh viên cùng danh sách quan hệ thành viên đội để tránh dùng dữ liệu phiên đã cũ.
        Student student = studentRepository.findByIdWithTeamMembers(
                userDetails.getAccount().getStudent().getStudentId()
        ).orElseThrow(() -> new BadRequestException("Tài khoản này không phải sinh viên, không có thông tin tham gia thi đấu"));

        // Giữ kiểm tra phòng vệ trong trường hợp nguồn dữ liệu trả về sinh viên rỗng.
        if (student == null) {
            throw new BadRequestException("Tài khoản này không phải sinh viên, không có thông tin tham gia thi đấu");
        }

        // Chọn đội đang thi đấu mà sinh viên hiện là thành viên.
        Team team = student.getTeamMembers().stream().map(TeamMember::getTeam).filter(t -> t.getStatus() == TeamStatus.BUSY).findFirst().orElseThrow(() -> new BadRequestException("Sinh viên không thuộc team nào đang hoạt động"));;
        // Lấy các đăng ký đã được ban tổ chức duyệt của đội này.
        List<Registration> approvedRegistrations = registrationRepository.findByTeam_TeamIdAndStatus(
                team.getTeamId(), RegistrationStatus.APPROVED);

        // Ghi nhận thời gian hiện tại một lần để dùng thống nhất trong toàn bộ phép lọc.
        LocalDateTime now = LocalDateTime.now();

        // Chỉ chọn đăng ký thuộc sự kiện đã bắt đầu và vẫn chưa kết thúc.
        Registration currentRegistration = approvedRegistrations.stream()
                .filter(r -> !now.isBefore(r.getHackathonEvent().getStartDate()) &&
                        !now.isAfter(r.getHackathonEvent().getEndDate()))
                .findFirst()
                .orElse(null);
        // Không có sự kiện đang diễn ra thì sinh viên chưa có thông tin thi đấu hiện tại.
        if (currentRegistration == null) {
            return null;
        }
        // Lấy sự kiện hiện tại từ đăng ký đã được xác định ở trên.
        HackathonEvent currentEvent = currentRegistration.getHackathonEvent();

        // Lấy toàn bộ lần tham gia các vòng của đội trong sự kiện đang diễn ra.
        List<TeamParticipant> teamParticipants = participantRepository.findParticipantByRegistration_Team_TeamIdAndRegistration_HackathonEvent_EventId(team.getTeamId(), currentEvent.getEventId());
        // Trả về rỗng khi đội chưa được tạo dữ liệu tham gia vòng nào.
        if(teamParticipants.isEmpty()){
            return null;
        }
        // Các lần tham gia của đội trong sự kiện dùng chung danh mục đã được phân.
        Category category = teamParticipants.get(0).getCategoryRound().getCategory();

        // Chuẩn bị danh sách trạng thái của đội theo từng vòng thi.
        List<RoundStatusDTO> list = new ArrayList<>();
        for(var teamParticipant : teamParticipants){
            list.add(this.mapToRoundStatusDTO(teamParticipant));
        }
        return CurrentParticipantDTO
                .builder()
                .eventID(currentEvent.getEventId())
                .eventName(currentEvent.getEventName())
                .categoryName(category.getCategoryName())
                .categoryId(category.getCategoryId())
                .rounds(list)
                .teamName(team.getTeamName())
                .build();

    }

    @Override
    // Lấy chi tiết tình trạng tham gia và bài nộp của đội trong một vòng cụ thể.
    public RoundParticipantDetailDTO getDetailParticipantByRound(Integer roundId, CustomUserDetails userDetails) {

        // Lấy hồ sơ ban tổ chức từ tài khoản hiện tại để kiểm soát quyền truy cập.
        EventCoordinator eventCoordinator = userDetails.getAccount().getEventCoordinator();
        // Chỉ ban tổ chức mới được xem toàn bộ đội và kết quả trong vòng.
        if(eventCoordinator == null){
            throw new BadRequestException("Bạn không có quyền truy cập. Bạn phải là eventcoordinator");
        }
        // Tìm vòng thi cần xem và báo lỗi rõ ràng khi mã vòng không tồn tại.
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy round với ID: " + roundId));

        // Lấy tất cả danh mục được tổ chức trong vòng thi này.
        List<CategoryRound> categoryRounds = categoryRoundRepository.findCategoryRoundByRound_RoundId(roundId);

        // Với mỗi danh mục, gom danh sách đội và thông tin kết quả của đội trong vòng.
        List<CategoryParticipantDTO> categoryDTOs = categoryRounds.stream().map(cr -> {
            // Lấy các đội thuộc đúng danh mục vòng đang được xử lý.
            List<TeamParticipant> participants = participantRepository.findByCategoryRound_CategoryRoundId(cr.getCategoryRoundId());

            List<ParticipantDetailDTO> participantList = participants.stream()
                    .map(p -> new ParticipantDetailDTO(
                            p.getId(),
                            p.getRegistration().getRegistrationId(),
                            p.getRegistration().getTeam().getTeamId(),
                            p.getRegistration().getTeam().getTeamName(),
                            p.getStatus(),
                            p.getDisqualificationReason(),
                            p.getSubmissionStatus(),
                            p.getTotalScore(),
                            p.getRank()
                    ))
                    .sorted(Comparator.comparingInt(c -> c.rank() != null ? c.rank() : Integer.MAX_VALUE))
                    .toList();

            return CategoryParticipantDTO.builder()
                    .roundName(round.getRoundName())
                    .roundIndex(round.getOrderIndex())
                    .categoryRoundId(cr.getCategoryRoundId())
                    .categoryName(cr.getCategory().getCategoryName())
                    .totalTeams(participants.size())
                    .participants(participantList)
                    .build();
        }).toList();
        return RoundParticipantDetailDTO.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .categories(categoryDTOs)
                .build();
    }

    private RoundStatusDTO mapToRoundStatusDTO(TeamParticipant participant){
        Round round = participant.getCategoryRound().getRound();
        return RoundStatusDTO.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .categoryRound(participant.getCategoryRound().getCategoryRoundId())
                .status(participant.getStatus())
                .roundStatus(round.getStatus())
                .evaluetionCriteria(round.getEvaluationCriterias())
                .SubmissionDeadline(round.getSubmissionDeadline())
                .StartTime(round.getStartTime())
                .submissionType(round.getSubmissionType())
                .EndTime(round.getEndTime())
                .build();
    }


}

