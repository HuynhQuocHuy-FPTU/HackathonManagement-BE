package com.hackathon.service;

import com.hackathon.dto.ExpertAssignedGroupDTO;
import com.hackathon.dto.ParticipantResponseDTO;
import com.hackathon.dto.ranking.CategoryRankingResponse;
import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.dto.ranking.RankingResponseDTO;
import com.hackathon.dto.round.RoundStatusDTO;
import com.hackathon.dto.team.CurrentParticipantDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.validator.DisqualifyValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
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
    private final RoundAdvancementService roundAdvancementService;
    private final RegistrationRepository registrationRepository;



    public List<ExpertAssignedGroupDTO> getAssignParticipants(Integer eventId, CustomUserDetails userDetails) {

        //1. Lấy Account đang đăng nhập
        Account account = userDetails.getAccount();

        // 2. Tìm expert tương ứng
        Expert expert = expertRepository
                .findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() ->
                        new BadRequestException(
                                "Không tìm thấy expert ứng với account " + account.getEmail()));

        //3. Lấy tất cả expertAssign của expert theo event
        List<ExpertAssign> assigns = expertAssignRepository.findExpertAssignments(expert.getExpertId(), eventId);

        if (assigns.isEmpty()) {
            throw new BadRequestException("Expert không được phân công trong event này");
        }

        //4. Lấy participant tương ứng
        return assigns.stream().map(this::buildGroup).toList();
    }

    public void disqualifyTeam(Integer eventId, Integer teamId, String reason) {
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Account account = userDetails.getAccount();
        // 1. Lấy toàn bộ Participant của Team này trong Event này
        List<TeamParticipant> teamParticipants = participantRepository.findParticipantByRegistration_Team_TeamIdAndRegistration_HackathonEvent_EventId(teamId, eventId);
        //3. Tìm event
        HackathonEvent event = hackathonEventRepository.findById(eventId).orElseThrow(() -> new BadRequestException("Không tìm thấy event"));

        // 2. Validate: team phải thuộc event này VÀ đăng ký phải đã được APPROVED
        teamParticipants = disqualifyValidator.validateTeamBelongsToEventAndApproved(teamParticipants, teamId, eventId);

        //Lưu lại teamparticipant passed ở round gần nhất mà team đã passed
        TeamParticipant mostRecentlyPassed = teamParticipants.stream().filter(p -> p.getStatus() == ParticipantStatus.PASSED).max(Comparator.comparing(p -> p.getCategoryRound().getRound().getOrderIndex())).orElseThrow(null);

        // 3. Đổi status từng Participant + lưu lý do loại
        for (TeamParticipant teamParticipant : teamParticipants) {
            teamParticipant.setStatus(ParticipantStatus.DISQUALIFIED);
            teamParticipant.setDisqualificationReason(reason);
        }
        participantRepository.saveAll(teamParticipants);

        // 4. Đổi status Team — loại hẳn khỏi event
        Team team = teamParticipants.get(0).getRegistration().getTeam();
        team.setStatus(TeamStatus.DRAFT);
        teamRepository.save(team);

        // Lấy round tiếp theo
        Round nextRound = roundRepository.findRoundByHackathonEvent_EventIdAndOrderIndex(eventId, mostRecentlyPassed.getCategoryRound().getRound().getOrderIndex() + 1).orElseThrow(null);

        if(nextRound != null && LocalDateTime.now().isBefore(nextRound.getStartTime())){
            roundAdvancementService.disqualifyRetroactively(mostRecentlyPassed, nextRound);
        }

        Account accountLeader = team.getTeamMembers()
                .stream()
                .filter(TeamMember::getIsLeader)
                .map(TeamMember::getStudent)
                .map(Student::getAccount)
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException("Không tìm thấy trưởng nhóm"));
        auditService.saveLog(account,
                AuditAction.DISQUALIFY_TEAM,
                AuditEntityType.PARTICIPANT,
                teamParticipants.get(0).getId(),
                team.getTeamName());

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

    public TeamParticipant saveParticipant(Registration registration) {
        TeamParticipant teamParticipant = new TeamParticipant();
        teamParticipant.setRegistration(registration);
        teamParticipant.setCategoryRound(null);
        teamParticipant.setStatus(ParticipantStatus.ACTIVE);

        return participantRepository.save(teamParticipant);
    }

    @Override
    public CurrentParticipantDTO getCurrentParticipant(CustomUserDetails userDetails) {
        Student student = userDetails.getAccount().getStudent();

        if (student == null) {
            throw new BadRequestException("Tài khoản này không phải sinh viên, không có thông tin tham gia thi đấu");
        }

        Team team = student.getTeamMembers().stream().map(TeamMember::getTeam).filter(t -> t.getStatus() == TeamStatus.BUSY).findFirst().orElseThrow(() -> new BadRequestException("Sinh viên không thuộc team nào đang hoạt động"));;
        List<Registration> approvedRegistrations = registrationRepository.findByTeam_TeamIdAndStatus(
                team.getTeamId(), RegistrationStatus.APPROVED);

        LocalDateTime now = LocalDateTime.now();

        // CHỈ LẤY ĐÚNG EVENT ĐANG DIỄN RA
        Registration currentRegistration = approvedRegistrations.stream()
                .filter(r -> !now.isBefore(r.getHackathonEvent().getStartDate()) &&
                        !now.isAfter(r.getHackathonEvent().getEndDate()))
                .findFirst()
                .orElse(null);
        if (currentRegistration == null) {
            return null;
        }
        HackathonEvent currentEvent = currentRegistration.getHackathonEvent();

        List<TeamParticipant> teamParticipants = participantRepository.findParticipantByRegistration_Team_TeamIdAndRegistration_HackathonEvent_EventId(team.getTeamId(), currentEvent.getEventId());
        if(teamParticipants.isEmpty()){
            return null;
        }
        Category category = teamParticipants.get(0).getCategoryRound().getCategory();

        List<RoundStatusDTO> list = new ArrayList<>();
        for(var teamParticipant : teamParticipants){
            list.add(this.mapToRoundStatusDTO(teamParticipant));
        }

        return CurrentParticipantDTO.builder().eventID(currentEvent.getEventId()).eventName(currentEvent.getEventName()).categoryName(category.getCategoryName()).categoryId(category.getCategoryId()).rounds(list).teamName(team.getTeamName()).build();

    }
    private RoundStatusDTO mapToRoundStatusDTO(TeamParticipant participant){
        Round round = participant.getCategoryRound().getRound();
        return RoundStatusDTO.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .status(participant.getStatus()).build();
    }


}

