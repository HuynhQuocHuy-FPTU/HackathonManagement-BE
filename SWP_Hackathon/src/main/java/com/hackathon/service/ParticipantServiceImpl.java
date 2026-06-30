package com.hackathon.service;

import com.hackathon.dto.ExpertAssignedGroupDTO;
import com.hackathon.dto.ParticipantResponseDTO;
import com.hackathon.dto.ranking.CategoryRankingResponse;
import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.dto.ranking.RankingResponseDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AuditAction;
import com.hackathon.entity.enums.AuditEntityType;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.entity.enums.TeamStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.validator.DisqualifyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final RoundRepository roundRepository;
    private final CategoryRoundRepository categoryRoundRepository;

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
        List<TeamParticipant> teamParticipants = participantRepository
                .findParticipantByRegistration_Team_TeamIdAndRegistration_HackathonEvent_EventId(teamId, eventId);
        //3. Tìm event
        HackathonEvent event = hackathonEventRepository.findById(eventId).orElseThrow(() -> new BadRequestException("Không tìm thấy event"));

        // 2. Validate: team phải thuộc event này VÀ đăng ký phải đã được APPROVED
        teamParticipants = disqualifyValidator.validateTeamBelongsToEventAndApproved(teamParticipants, teamId, eventId);

        // 3. Đổi status từng Participant + lưu lý do loại
        for (TeamParticipant teamParticipant : teamParticipants) {
            teamParticipant.setStatus(ParticipantStatus.DISQUALIFIED); // điều chỉnh đúng tên enum thật
            teamParticipant.setDisqualificationReason(reason);
        }
        participantRepository.saveAll(teamParticipants);

        // 4. Đổi status Team — loại hẳn khỏi event
        Team team = teamParticipants.get(0).getRegistration().getTeam();
        team.setStatus(TeamStatus.DRAFT);
        teamRepository.save(team);

        Account accountLeader = team.getTeamMembers()
                .stream()
                .filter(TeamMember::getIsLeader)
                .map(TeamMember::getStudent)
                .map(Student::getAccount)
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException("Không tìm thấy trưởng nhóm"));
        auditService.saveLog(account, AuditAction.DISQUALIFY_TEAM, AuditEntityType.PARTICIPANT, teamParticipants.get(0).getId(), team.getTeamName());

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


    //===============================================//
    //RANKING
    //===============================================//


    @Override
    public CategoryRoundRankingResponse getRankingByEventCoordinator(
            Integer roundId, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Event Coordinator."));

        Round round = roundRepository.findById(roundId).orElseThrow(
                () -> new BadRequestException("Không tìm thấy round tương ứng với ID này."));

        List<CategoryRankingResponse> categoriesRanking = new ArrayList<>();
        CategoryRankingResponse response = null;
        for (CategoryRound cr : round.getCategoryRounds()) {
            // Thông qua Category Round lấy các Team đnag tham gia cuộc thi
            List<TeamParticipant> teamParticipants = cr.getTeamParticipants();
            teamParticipants.sort(Comparator.comparing(TeamParticipant::getRank));

            List<RankingResponseDTO> rankingResponse = new ArrayList<>();
            for (TeamParticipant participant : teamParticipants) {
                String teamName = (participant.getRegistration() != null) ? participant.getRegistration().getTeam().getTeamName() : "N/A";
                RankingResponseDTO dto = RankingResponseDTO.builder()
                        .participantId(participant.getId())
                        .totalScore(participant.getTotalScore())
                        .rank(participant.getRank())
                        .teamName(teamName)
                        .status(participant.getStatus())
                        .build();
                rankingResponse.add(dto);
            }
            response = CategoryRankingResponse.builder()
                    .categoryRoundId(cr.getCategoryRoundId())
                    .categoryId(cr.getCategory().getCategoryId())
                    .categoryName(cr.getCategory().getCategoryName())
                    .teams(rankingResponse)
                    .build();
            categoriesRanking.add(response);
        }


        return CategoryRoundRankingResponse.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .orderIndex(round.getOrderIndex())
                .advancementRule(round.getAdvancementRule())
                .topN(round.getTopN())
                .roundStatus(round.getStatus())
                .categoriesRanking(categoriesRanking).build();
    }


}
