package com.hackathon.service;

import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.RegistrationStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DrawResultServiceImpl implements DrawResultService{
    private final CategoryRepository categoryRepository;
    private final RoundRepository roundRepository;
    private final CategoryRoundRepository categoryRoundRepository;
    private final ParticipantRepository participantRepository;
    private final RegistrationRepository registrationRepository;

    @Override
    public List<TeamParticipant> importDrawResults(Integer eventId, List<DrawResultRequestDTO> drawResults) {
        if(drawResults == null && drawResults.isEmpty()){
            throw new BadRequestException("Danh sách kết quả bốc thăm không được rỗng");
        }

        //Tìm round đầu tiên có index nhỏ nhất trong event

        Round firstRound = roundRepository.findFirstByHackathonEvent_EventIdOrderByOrderIndexAsc(eventId).orElseThrow(() -> new BadRequestException("Event" + eventId + "chưa có round nào"));

        List<TeamParticipant> updateParticipants = new ArrayList<>();
        for(DrawResultRequestDTO request : drawResults){
            // 1. Lấy registration đã có trong participant và thuộc đúng event

            Registration registration = registrationRepository.findRegistrationByRegistrationIdAndHackathonEvent_EventId(request.getRegistrationId(), eventId).orElseThrow(() -> new BadRequestException("Không tìm thấy registration: " + request.getRegistrationId() + "thuộc event có id: " + eventId));

            if(registration.getStatus() != RegistrationStatus.APPROVED){
                throw new BadRequestException("Registration " + request.getRegistrationId() + "chưa được approve");
            }

            //2. Lấy category, thuộc đúng event
            Category category = categoryRepository.findCategoryByCategoryIdAndHackathonEvent_EventId(request.getCategoryId(), eventId).orElseThrow(() -> new BadRequestException("Không tìm thấy category: " + request.getCategoryId() + "với eventID: " + eventId));

            //3. Lấy participant đã được approve

            TeamParticipant participant = participantRepository.findParticipantByRegistration_RegistrationId(registration.getRegistrationId()).orElseThrow(() -> new BadRequestException("Không tìm thấy participant theo registration id" + request.getRegistrationId()));

            if(participant.getCategoryRound() != null){
                throw new BadRequestException("Registration đã được gán vào Category rồi" + request.getRegistrationId());
            }

            //4. Tìm category round của category ở round đầu tiên
            CategoryRound categoryRound = categoryRoundRepository.findCategoryRoundByCategory_CategoryIdAndRound_RoundId(request.getCategoryId(), firstRound.getRoundId()).orElseThrow(() -> new BadRequestException("Chưa có CategoryRound cho category " + category.getCategoryId() +
                    " ở round đầu tiên " + firstRound.getRoundId()));

            //5. Cập nhật Participant set categoryRound
            participant.setCategoryRound(categoryRound);
            participant = participantRepository.save(participant);

            updateParticipants.add(participant);

        }
        return updateParticipants;
    }
}
