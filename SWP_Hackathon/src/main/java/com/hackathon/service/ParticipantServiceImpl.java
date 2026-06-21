package com.hackathon.service;

import com.hackathon.dto.ExpertAssignedGroupDTO;
import com.hackathon.dto.ParticipantResponseDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.entity.enums.TeamStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.ExpertAssignRepository;
import com.hackathon.repository.ExpertRepository;
import com.hackathon.repository.ParticipantRepository;
import com.hackathon.repository.TeamRepository;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipantServiceImpl {
    private final ExpertRepository expertRepository;
    private final ExpertAssignRepository expertAssignRepository;
    private final ParticipantRepository participantRepository;
    private final TeamRepository teamRepository;

    public List<ExpertAssignedGroupDTO> getAssignParticipants(Integer eventId){

        //1. Lấy Account đang đăng nhập, tìm expert
        Account currentAccount = getCurrentAccount();

        Expert expert = expertRepository.findByAccount_AccountId(currentAccount.getAccountId()).orElseThrow(() -> new BadRequestException("Không tìm thấy expert ứng với account" + currentAccount.getEmail()));

        //2. Lấy tất cả expertAssign của expert theo event
        List<ExpertAssign> assigns = expertAssignRepository.findExpertAssignments(expert.getExpertId(), eventId);

        if(assigns.isEmpty()){
            throw new BadRequestException("Expert không được phân công trong event này");
        }

        //3. Lấy participant tương ứng
        return assigns.stream().map(this::buildGroup).toList();
    }
    @Transactional
    public void disqualifyTeam(Integer eventId, Integer teamId, String reason) {

        // 1. Lấy toàn bộ Participant của Team này trong Event này
        List<Participant> participants = participantRepository.findParticipantByRegistration_Team_TeamIdAndRegistration_HackathonEvent_EventId(teamId, eventId);

        if (participants.isEmpty()) {
            throw new BadRequestException("Team " + teamId + " không tham gia event " + eventId);
        }

        // 2. Đổi status từng Participant + lưu lý do loại
        for (Participant participant : participants) {
            participant.setStatus(ParticipantStatus.DISQUALIFIED); // điều chỉnh đúng tên enum thật
            participant.setDisqualificationReason(reason);
        }
        participantRepository.saveAll(participants);

        // 3. Đổi status Team — loại hẳn khỏi event
        Team team = participants.get(0).getRegistration().getTeam();
        team.setStatus(TeamStatus.DRAFT); // điều chỉnh đúng tên enum thật
        teamRepository.save(team);

    }

    private ParticipantResponseDTO mapToResponse(Participant participant){
        if(participant == null){
            return null;
        }
        String teamName = participant.getRegistration().getTeam().getTeamName();

        return ParticipantResponseDTO.builder()
                .participantId(participant.getId())
                .teamName(teamName)
                .totalScore(participant.getTotalScore())
                .rank(participant.getRank())
                .status(participant.getStatus())
                .build();
    }

    private ExpertAssignedGroupDTO buildGroup(ExpertAssign expertAssign){
        CategoryRound categoryRound = expertAssign.getCategoryRound();

        List<Participant> participants = participantRepository.findParticipantByCategoryRound_CategoryRoundId(categoryRound.getCategoryRoundId());

        List<ParticipantResponseDTO> participantResponseDTOS = participants.stream().map(this::mapToResponse).toList();

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

    private Account getCurrentAccount(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken){
            throw new BadRequestException("Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại!");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            throw new BadRequestException("Không xác định được thông tin tài khoản đăng nhập");
        }

        return ((CustomUserDetails) principal).getAccount();
    }


}
