package com.hackathon.service.prize;

import com.hackathon.dto.event.Prize;
import com.hackathon.dto.event.PrizeRequestDTO;
import com.hackathon.dto.event.PrizeResponseDTO;
import com.hackathon.entity.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrizeServiceImpl {
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final RoundRepository roundRepository;
    private final ParticipantRepository participantRepository;
    private final HackathonEventRepository hackathonEventRepository;
    private final StudentRepository studentRepository;


    @Transactional
    public void assignPrize(CustomUserDetails userDetails, Integer eventId, List<PrizeRequestDTO> request) {
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(userDetails.getAccount().getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là EventCoordinator."));


        // Lấy round chung kết để gán giải thưởng
        Round finalRound = roundRepository.findFinalRoundByEventId(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng chung kết."));

        // Lấy ds giải thưởng
        List<Prize> prizes = finalRound.getHackathonEvent().getDescription().prizes();

        List<TeamParticipant> rankings = participantRepository.findByRoundId(finalRound.getRoundId());


        //1.Dựa vào rank để xếp giải thưởng tự động
        if (!rankings.isEmpty() && prizes != null && !prizes.isEmpty()) {

            for (int i = 0; i < Math.min(finalRound.getTopN(), Math.min(rankings.size(), prizes.size())); i++) {

                TeamParticipant team = rankings.get(i);
                Prize prize = prizes.get(i);
                team.setAward(prize.reward());
                team.setTitleAward(prize.title());
            }
            participantRepository.saveAll(rankings);
        }

        //2.BTC gán giải thưởng ngoại lệ

        if (request != null && !request.isEmpty()) {
            for (PrizeRequestDTO rq : request) {
                TeamParticipant tp = participantRepository.findById(rq.getTeamParticipantId())
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin về đội thi này."));
                System.out.println(
                        tp.getId() +
                                " rank=" + tp.getRank()
                );
                // Check có giải thưởng trước đó chưa
                tp.setTitleAward(appendValue(tp.getTitleAward(), rq.getPrizeTitle()));
                tp.setAward(appendValue(tp.getAward(), rq.getPrizeReward()));
                participantRepository.save(tp);
            }

        }

    }

    public PrizeResponseDTO getPrize(CustomUserDetails userDetails, Integer eventId) {
        Account account = userDetails.getAccount();
        Student student = studentRepository.findByAccount_AccountId(userDetails.getAccount().getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là sinh viên."));
        // Tìm round chung kết để lấy giải thưởng
        Round finalRound = roundRepository.findFinalRoundByEventId(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm vòng thi."));
        HackathonEvent event = hackathonEventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));
        List<Prize> prizeList = finalRound.getHackathonEvent().getDescription().prizes();
        TeamParticipant participant = participantRepository
                .findByRoundId(finalRound.getRoundId()).stream()
                .filter(tp -> tp.getRegistration().getTeam().getTeamMembers().stream()
                        .anyMatch(member -> member.getStudent().getStudentId()
                                == student.getStudentId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Bạn không thuộc đội tham gia sự kiện này."));
        // Lấy giải thưởng từ round cuối ra
        PrizeResponseDTO prizeResponseDTO = new PrizeResponseDTO();
        prizeResponseDTO.setEventId(eventId);
        prizeResponseDTO.setEventName(event.getEventName());
        prizeResponseDTO.setRoundId(finalRound.getRoundId());
        prizeResponseDTO.setRoundName(finalRound.getRoundName());

        String name = participant.getRegistration().getTeam().getTeamName();
        prizeResponseDTO.setPrizeReward(participant.getAward());
        prizeResponseDTO.setPrizeTitle(participant.getTitleAward());
        prizeResponseDTO.setRanking(participant.getRank());
        prizeResponseDTO.setTeamParticipantId(participant.getId());
        prizeResponseDTO.setTeamName(name);
        return prizeResponseDTO;

    }

    public List<PrizeResponseDTO> getPrizeForCoordinator(CustomUserDetails userDetails, Integer eventId) {
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(userDetails.getAccount().getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức."));
        // Tìm round chung kết để lấy giải thưởng
        Round finalRound = roundRepository.findFinalRoundByEventId(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm vòng thi."));
        HackathonEvent event = hackathonEventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));
        List<TeamParticipant> participants = participantRepository.findByRoundId(finalRound.getRoundId());

        List<PrizeResponseDTO> result = new ArrayList<>();
        for (TeamParticipant participant : participants) {

            if (participant.getAward() == null && participant.getTitleAward() == null) {
                continue;
            }
            // Lấy giải thưởng từ round cuối ra
            PrizeResponseDTO prizeResponseDTO = new PrizeResponseDTO();
            prizeResponseDTO.setEventId(eventId);
            prizeResponseDTO.setEventName(event.getEventName());
            prizeResponseDTO.setRoundId(finalRound.getRoundId());
            prizeResponseDTO.setRoundName(finalRound.getRoundName());

            String name = participant.getRegistration().getTeam().getTeamName();
            prizeResponseDTO.setPrizeReward(participant.getAward());
            prizeResponseDTO.setPrizeTitle(participant.getTitleAward());
            prizeResponseDTO.setRanking(participant.getRank());
            prizeResponseDTO.setTeamParticipantId(participant.getId());
            prizeResponseDTO.setTeamName(name);
            result.add(prizeResponseDTO);

        }
        return result;
    }

    private String appendValue(String current, String newValue) {
        if (current == null || current.isEmpty()) return newValue;
        return current + ", " + newValue;
    }
}