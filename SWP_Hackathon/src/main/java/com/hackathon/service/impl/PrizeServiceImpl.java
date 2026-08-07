package com.hackathon.service.impl;

import com.hackathon.dto.event.Prize;
import com.hackathon.dto.event.PrizeRequestDTO;
import com.hackathon.dto.event.PrizeResponseDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
// Gán giải thưởng theo thứ hạng và cho phép ban tổ chức bổ sung giải ngoại lệ.
public class PrizeServiceImpl {
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final RoundRepository roundRepository;
    private final ParticipantRepository participantRepository;
    private final HackathonEventRepository hackathonEventRepository;
    private final StudentRepository studentRepository;


    @Transactional
    // Gán giải cho các đội ở vòng chung kết dựa trên thứ hạng và yêu cầu bổ sung.
    public void assignPrize(CustomUserDetails userDetails, Integer eventId, List<PrizeRequestDTO> request) {
        // Xác nhận người thao tác thuộc ban tổ chức.
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(userDetails.getAccount().getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là EventCoordinator."));

        // Tìm vòng cuối cùng của sự kiện để xác định kết quả trao giải.
        Round finalRound = roundRepository.findFinalRoundByEventId(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng chung kết."));

        // Lấy danh sách giải thưởng được cấu hình trong mô tả sự kiện.
        List<Prize> prizes = finalRound.getHackathonEvent().getDescription().prizes();
        
        // Chỉ chọn các đội đã vượt qua vòng cuối cùng để xét giải theo thứ hạng.
        List<TeamParticipant> rankings = participantRepository.findByRoundId(finalRound.getRoundId())
                .stream()
                .filter(rank -> rank.getStatus() == ParticipantStatus.PASSED).toList();
        // Chỉ gán tự động khi đồng thời có đội chiến thắng và cấu hình giải thưởng.
        if (!rankings.isEmpty() && prizes != null && !prizes.isEmpty()) {

            // Số lần lặp không vượt quá số đội thắng, số giải và giới hạn đội được chọn.
            for (int i = 0; i < Math.min(finalRound.getTopN(), Math.min(rankings.size(), prizes.size())); i++) {

                TeamParticipant team = rankings.get(i);
                Prize prize = prizes.get(i);
                // Gán phần thưởng và danh hiệu cùng vị trí trong danh sách giải cho đội.
                team.setAward(prize.reward());
                team.setTitleAward(prize.title());
            }
            // Lưu đồng loạt kết quả trao giải tự động.
            participantRepository.saveAll(rankings);
        }

        // Ban tổ chức có thể bổ sung giải ngoại lệ ngoài danh sách được gán tự động.
        if (request != null && !request.isEmpty()) {
            // Xử lý từng yêu cầu bổ sung giải cho một đội cụ thể.
            for (PrizeRequestDTO rq : request) {
                TeamParticipant tp = participantRepository.findById(rq.getTeamParticipantId())
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin về đội thi này."));
                // Nối giải mới vào dữ liệu hiện có để không làm mất giải đã nhận trước đó.
                tp.setTitleAward(appendValue(tp.getTitleAward(), rq.getPrizeTitle()));
                tp.setAward(appendValue(tp.getAward(), rq.getPrizeReward()));
                // Lưu ngay thay đổi của đội đang được bổ sung giải.
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

    // Nối giải bổ sung vào giá trị cũ mà không làm mất giải đã được gán trước đó.
    private String appendValue(String current, String newValue) {
        if (current == null || current.isEmpty()) return newValue;
        return current + ", " + newValue;
    }
}
