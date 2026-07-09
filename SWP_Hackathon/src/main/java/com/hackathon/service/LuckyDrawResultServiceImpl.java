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
    @Transactional
    @Override
    public List<TeamParticipant> importDrawResults(Integer eventId, List<DrawResultRequestDTO> drawResults, CustomUserDetails userDetails, Integer responseDeadline) {
        Account acc = userDetails.getAccount();

        // Tìm event
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy event"));

        if (event.getWorkshopStatus() != WorkshopStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể gán kết quả bốc thăm sau khi Workshop đã hoàn thành!");
        }

        if (drawResults == null || drawResults.isEmpty()) {
            throw new BadRequestException("Danh sách kết quả bốc thăm không được rỗng");
        }

        // Tìm round đầu tiên
        Round firstRound = roundRepository.findFirstByHackathonEvent_EventIdOrderByOrderIndexAsc(eventId)
                .orElseThrow(() -> new BadRequestException("Event " + eventId + " chưa có round nào"));

        List<TeamParticipant> updateTeamParticipants = new ArrayList<>();

        // Vòng lặp chính: Duyệt qua từng nhóm kết quả (Category + List Registration)
        for (DrawResultRequestDTO drawResult : drawResults) {

            Integer categoryId = drawResult.getCategoryId();

            // 2. Lấy category, thuộc đúng event (làm 1 lần ngoài vòng lặp registration)
            Category category = categoryRepository.findCategoryByCategoryIdAndHackathonEvent_EventId(categoryId, eventId)
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy category: " + categoryId + " với eventID: " + eventId));

            // 4. Tìm category round ở round đầu tiên (làm 1 lần ngoài vòng lặp registration)
            CategoryRound categoryRound = categoryRoundRepository.findCategoryRoundByCategory_CategoryIdAndRound_RoundId(categoryId, firstRound.getRoundId())
                    .orElseThrow(() -> new BadRequestException("Chưa có CategoryRound cho category " + categoryId + " ở round đầu tiên"));

            // Duyệt qua từng registrationId trong category đó
            for (Integer registrationId : drawResult.getRegistrationId()) {

                // 1. Lấy registration
                Registration registration = registrationRepository.findRegistrationByRegistrationIdAndHackathonEvent_EventId(registrationId, eventId)
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy registration: " + registrationId + " thuộc event: " + eventId));

                if (registration.getStatus() != RegistrationStatus.APPROVED) {
                    throw new BadRequestException("Registration " + registrationId + " chưa được approve");
                }

                // 3. Lấy participant
                TeamParticipant teamParticipant = participantRepository.findParticipantByRegistration_RegistrationId(registration.getRegistrationId())
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy participant theo registration id " + registrationId));

                if (teamParticipant.getCategoryRound() != null) {
                    throw new BadRequestException("Registration " + registrationId + " đã được gán vào Category rồi");
                }

                // 5. Cập nhật Participant
                teamParticipant.setCategoryRound(categoryRound);
                teamParticipant = participantRepository.save(teamParticipant);
                updateTeamParticipants.add(teamParticipant);

                // 6. Thông báo
                Team team = teamParticipant.getRegistration().getTeam();
                Account accountLeader = team.getTeamMembers()
                        .stream()
                        .filter(TeamMember::getIsLeader)
                        .map(TeamMember::getStudent)
                        .map(Student::getAccount)
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy trưởng nhóm của team " + team.getTeamName()));

                notificationService.notifyAssignedCategory(acc, accountLeader, team.getTeamName(), event.getEventName(), category.getCategoryName(), responseDeadline);
            }
        }

        return updateTeamParticipants;
    }


}
