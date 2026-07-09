package com.hackathon.service;

import com.hackathon.dto.team.AdvancedTeamDTO;
import com.hackathon.dto.team.CategoryAdvancementResultDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.CategoryRoundRepository;
import com.hackathon.repository.EvaluationRepository;
import com.hackathon.repository.ParticipantRepository;
import com.hackathon.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoundAdvancementService {
    private final CategoryRoundRepository categoryRoundRepository;
    private final RoundRepository roundRepository;
    private final EvaluationRepository evaluationRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantService participantService;

    @Transactional
    public List<AdvancedTeamDTO> advanceTopTeams(Integer currentCategoryRoundId){
        CategoryRound currentCategoryRound = categoryRoundRepository.findById(currentCategoryRoundId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy category round hiện tại."));

        Round currentRound = currentCategoryRound.getRound();

        //Chặn thăng vòng nếu trong category của round đó có bài chưa chấm (NOT_GRADE)
        assertGradingComplete(currentCategoryRoundId);

        //Tính totalScore cho mỗi team từ nhiều ban giám khảo cham (trung bình điểm từ các evaluation đã GRADE), sắp xếp giảm dần
       List<TeamParticipant> rankedParticipants = recalculateScoresForCategoryRound(currentCategoryRoundId);

        int effectiveTopN = resolveTopN(currentRound);

       //tìm categoryRound tương ứng của category trước đó ở vòng tiếp theo
        CategoryRound nextCategoryRound = this.findNextCategoryRound(currentCategoryRound);

        List<AdvancedTeamDTO> result = new ArrayList<>();

        for(int i = 0; i < rankedParticipants.size() ; i ++){
            TeamParticipant current = rankedParticipants.get(i);
            boolean isAdvancing = i < effectiveTopN;

            if(isAdvancing){
                current.setStatus(ParticipantStatus.PASSED);
                participantRepository.save(current);

                Registration registration = current.getRegistration();

                boolean alreadyAdvanced = participantRepository.existsByCategoryRound_CategoryRoundIdAndRegistration_RegistrationId(currentCategoryRoundId, registration.getRegistrationId());

                if(alreadyAdvanced){
                    log.info("Team " + registration.getTeam().getTeamName() + "đã được thăng vòng trước đó, bỏ qua");
                    continue;
                }
                TeamParticipant nextParticipant = TeamParticipant.builder()
                        .status(ParticipantStatus.ACTIVE)
                        .totalScore(null)
                        .categoryRound(nextCategoryRound)
                        .registration(registration)
                        .build();

                TeamParticipant savedTeamParticipant = participantRepository.save(nextParticipant);
                Team team = registration.getTeam();
                result.add(new AdvancedTeamDTO(team.getTeamId(), team.getTeamName(), current.getTotalScore(), savedTeamParticipant.getId()));
            }else{
                current.setStatus(ParticipantStatus.FAILED);
                participantRepository.save(current);
            }
        }
        return result;
    }

    @Transactional
    public List<CategoryAdvancementResultDTO> advanceAllCategoriesInRound(Integer roundId) {
        List<CategoryRound> categoryRounds = categoryRoundRepository.findCategoryRoundByRound_RoundId(roundId);

        if (categoryRounds.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy category nào thuộc round này");
        }

        List<CategoryAdvancementResultDTO> results = new ArrayList<>();

        for (CategoryRound categoryRound : categoryRounds) {
            int categoryRoundId = categoryRound.getCategoryRoundId();

            try {
                List<AdvancedTeamDTO> advanced = advanceTopTeams(categoryRoundId);
                results.add(new CategoryAdvancementResultDTO(categoryRoundId, advanced, null));
            } catch (Exception e) {
                log.error("Lỗi khi thăng vòng cho categoryRound {}: {}", categoryRoundId, e.getMessage());
                results.add(new CategoryAdvancementResultDTO(categoryRoundId, List.of(), e.getMessage()));
            }
        }
        return results;
    }



    private CategoryRound findNextCategoryRound(CategoryRound currentCategoryRound) {
        Category category = currentCategoryRound.getCategory();
        Round currentRound = currentCategoryRound.getRound();

        Round nextRound = roundRepository
                .findRoundByHackathonEvent_EventIdAndOrderIndex(
                        currentRound.getHackathonEvent().getEventId(),
                        currentRound.getOrderIndex() + 1
                )
                .orElseThrow(() -> new BadRequestException("Đây đã là vòng cuối cùng, không có vòng tiếp theo"));

        return categoryRoundRepository
                .findCategoryRoundByCategory_CategoryIdAndRound_RoundId(category.getCategoryId(), nextRound.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa cấu hình category này cho vòng tiếp theo (thiếu CategoryRound)"));
    }

    public void disqualifyRetroactively(Integer teamParticipantId){

    }

    private List<TeamParticipant> recalculateScoresForCategoryRound(Integer categoryRoundId) {
        List<TeamParticipant> allActiveParticipants = participantRepository.findByCategoryRound_CategoryRoundIdAndStatus(categoryRoundId, ParticipantStatus.ACTIVE);

        for (TeamParticipant participant : allActiveParticipants) {
            participantService.calculateTotalScore(participant);
        }

        allActiveParticipants.sort(Comparator.comparing(TeamParticipant::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder())));

        int rank = 1;
        for(TeamParticipant teamParticipant : allActiveParticipants){
            teamParticipant.setRank(rank++);
            participantRepository.save(teamParticipant);
        }

        return allActiveParticipants;

    }

    private int resolveTopN(Round currentRound) {
        if (currentRound.getTopN() != null && currentRound.getTopN() > 0) {
            return currentRound.getTopN();
        }

        throw new BadRequestException(
                "Round chưa được cấu hình Top_N. Vui lòng cập nhật Top_N cho round trước khi thăng vòng.");
    }

    private void assertGradingComplete(Integer categoryRoundId) {
        boolean hasUngraded = evaluationRepository.existsBySubmission_TeamParticipant_CategoryRound_CategoryRoundIdAndStatus(
                        categoryRoundId, EvaluationStatus.NOT_GRADED);

        if (hasUngraded) {
            throw new BadRequestException(
                    "Category round " + categoryRoundId + " vẫn còn bài chưa được chấm điểm (NOT_GRADED). "
                            + "Vui lòng chấm xong hết trước khi thăng vòng.");
        }
    }

}
