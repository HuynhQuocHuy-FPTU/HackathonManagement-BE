package com.hackathon.service;

import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.RegistrationStatus;
import com.hackathon.entity.enums.WorkshopStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class LuckyDrawResultServiceImpl implements LuckyDrawResultService {
    private final CategoryRepository categoryRepository;
    private final RoundRepository roundRepository;
    private final CategoryRoundRepository categoryRoundRepository;
    private final ParticipantRepository participantRepository;
    private final RegistrationRepository registrationRepository;
    private final HackathonEventRepository eventRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    @Transactional
    @Override
    public List<TeamParticipant> importDrawResults(Integer eventId, DrawResultRequestDTO drawResults, CustomUserDetails userDetails, Integer responseDeadline) {
        Account acc = userDetails.getAccount();
        //tìm event
        HackathonEvent event = eventRepository.findById(eventId).orElseThrow(() -> new BadRequestException("Không tìm thấy event"));
        if (event.getWorkshopStatus() != WorkshopStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể gán kết quả bốc thăm sau khi Workshop đã hoàn thành!");
        }
        if (drawResults == null ||
                drawResults.getRegistrationId() == null ||
                drawResults.getRegistrationId().isEmpty()) {

            throw new BadRequestException("Danh sách kết quả bốc thăm không được rỗng");
        }

        //Tìm round đầu tiên có index nhỏ nhất trong event
        Round firstRound = roundRepository.findFirstByHackathonEvent_EventIdOrderByOrderIndexAsc(eventId).orElseThrow(() -> new BadRequestException("Event" + eventId + "chưa có round nào"));

        List<TeamParticipant> updateTeamParticipants = new ArrayList<>();
        List<Account> accLeaders = new ArrayList<>();
        for(Integer registrationId : drawResults.getRegistrationId()){
            // 1. Lấy registration đã có trong participant và thuộc đúng event
            Registration registration = registrationRepository.findRegistrationByRegistrationIdAndHackathonEvent_EventId(registrationId, eventId).orElseThrow(() -> new BadRequestException("Không tìm thấy registration: " + registrationId + "thuộc event có id: " + eventId));

            if(registration.getStatus() != RegistrationStatus.APPROVED){
                throw new BadRequestException("Registration " + registrationId + "chưa được approve");
            }

            //2. Lấy category, thuộc đúng event
            Category category = categoryRepository.findCategoryByCategoryIdAndHackathonEvent_EventId(drawResults.getCategoryId(), eventId).orElseThrow(() -> new BadRequestException("Không tìm thấy category: " + drawResults.getCategoryId() + "với eventID: " + eventId));

            //3. Lấy participant đã được approve
            TeamParticipant teamParticipant = participantRepository.findParticipantByRegistration_RegistrationId(registration.getRegistrationId()).orElseThrow(() -> new BadRequestException("Không tìm thấy participant theo registration id " + registrationId));

            if(teamParticipant.getCategoryRound() != null){
                throw new BadRequestException("Registration đã được gán vào Category rồi" + registrationId);
            }

            //4. Tìm category round của category ở round đầu tiên
            CategoryRound categoryRound = categoryRoundRepository.findCategoryRoundByCategory_CategoryIdAndRound_RoundId(category.getCategoryId(), firstRound.getRoundId()).orElseThrow(() -> new BadRequestException("Chưa có CategoryRound cho category " + category.getCategoryId() +
                    " ở round đầu tiên " + firstRound.getRoundId()));

            //5. Cập nhật Participant set categoryRound
            teamParticipant.setCategoryRound(categoryRound);
            teamParticipant = participantRepository.save(teamParticipant);
            updateTeamParticipants.add(teamParticipant);

            //6. Lấy ra team leader account
            Team team = teamParticipant.getRegistration().getTeam();
            Account accountLeader = team.getTeamMembers()
                    .stream()
                    .filter(TeamMember::getIsLeader)
                    .map(TeamMember::getStudent)
                    .map(Student::getAccount)
                    .findFirst()
                    .orElseThrow(() ->
                            new BadRequestException("Không tìm thấy trưởng nhóm"));

            notificationService.notifyAssignedCategory(acc, accountLeader, team.getTeamName(), event.getEventName(),category.getCategoryName(), responseDeadline);
        }

        return updateTeamParticipants;
    }


}
