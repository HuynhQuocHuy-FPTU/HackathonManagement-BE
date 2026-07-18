package com.hackathon.service.prize;

import com.hackathon.dto.event.Prize;
import com.hackathon.dto.event.PrizeRequestDTO;
import com.hackathon.entity.EventCoordinator;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.Round;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.EventCoordinatorRepository;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.repository.ParticipantRepository;
import com.hackathon.repository.RoundRepository;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrizeServiceImpl {
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final RoundRepository roundRepository;
    private final ParticipantRepository participantRepository;

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

    private String appendValue(String current, String newValue) {
        if (current == null || current.isEmpty()) return newValue;
        return current + ", " + newValue;
    }
}