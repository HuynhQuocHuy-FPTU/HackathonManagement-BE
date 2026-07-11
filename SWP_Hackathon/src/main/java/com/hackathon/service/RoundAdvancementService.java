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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final int SCORE_SCALE = 2;

    @Transactional
    public List<AdvancedTeamDTO> advanceTopTeams(Integer currentCategoryRoundId) {
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

        for (int i = 0; i < rankedParticipants.size(); i++) {
            TeamParticipant current = rankedParticipants.get(i);
            boolean isAdvancing = i < effectiveTopN;

            if (isAdvancing) {
                current.setStatus(ParticipantStatus.PASSED);
                participantRepository.save(current);

                Registration registration = current.getRegistration();

                boolean alreadyAdvanced = participantRepository.existsByCategoryRound_CategoryRoundIdAndRegistration_RegistrationId(currentCategoryRoundId, registration.getRegistrationId());

                if (alreadyAdvanced) {
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
            } else {
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

    public void disqualifyRetroactively(TeamParticipant oldTeamParticipant, Round nextRound) {
        CategoryRound categoryRound = oldTeamParticipant.getCategoryRound();
        Round currentRound = categoryRound.getRound();

        //nếu team bị loại đã pass ở vòng trước, thì cần lấy đội failed có điểm cao nhất
        if (oldTeamParticipant.getStatus() == ParticipantStatus.PASSED) {
            List<TeamParticipant> teamParticipantFailed = categoryRound.getTeamParticipants().stream().filter(p -> p.getStatus() == ParticipantStatus.FAILED).toList();
            teamParticipantFailed.sort(Comparator.comparing(TeamParticipant::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder())));

            if (!teamParticipantFailed.isEmpty()) {
                TeamParticipant replaceTeam = teamParticipantFailed.getFirst();
                replaceTeam.setStatus(ParticipantStatus.PASSED);
                participantRepository.save(replaceTeam);

                // 4. Tạo bản ghi cho đội mới ở vòng tiếp theo
                Registration registration = replaceTeam.getRegistration();
                CategoryRound nextCategoryRound = findNextCategoryRound(categoryRound);

                TeamParticipant nextParticipant = TeamParticipant.builder()
                        .status(ParticipantStatus.ACTIVE)
                        .categoryRound(nextCategoryRound)
                        .registration(registration)
                        .build();
                participantRepository.save(nextParticipant);

                log.info("Đội {} đã được đôn lên thay thế.", registration.getTeam().getTeamName());
            }
        }

        // 5. Quan trọng: Cập nhật lại Ranking cho toàn bộ CategoryRound đó
        // Sau khi loại 1 đội và đôn 1 đội, thứ hạng hiện tại đã thay đổi
        calculateRanking(
                participantRepository.findByCategoryRound_CategoryRoundIdAndStatusIsNotIn(
                        categoryRound.getCategoryRoundId(),
                        List.of(ParticipantStatus.DISQUALIFIED, ParticipantStatus.WITHDRAWN)
                )
        );
    }
    private List<TeamParticipant> recalculateScoresForCategoryRound(Integer categoryRoundId) {
        // 1. Lấy danh sách hợp lệ
        List<TeamParticipant> participants = participantRepository.findByCategoryRound_CategoryRoundIdAndStatusIsNotIn(
                categoryRoundId,
                List.of(ParticipantStatus.DISQUALIFIED, ParticipantStatus.WITHDRAWN)
        );

        // 2. Tính toán điểm số
        for (TeamParticipant participant : participants) {
            this.calculateTotalScore(participant);
        }

        // Lưu các thay đổi về điểm số (nếu có)
        participantRepository.saveAll(participants);

        // 3. Gọi hàm xếp hạng sau khi đã có điểm
        return calculateRanking(participants);
    }

    private List<TeamParticipant> calculateRanking(List<TeamParticipant> participants) {
        // 1. Sắp xếp theo điểm số (giảm dần)
        participants.sort(Comparator.comparing(
                TeamParticipant::getTotalScore,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        // 2. Gán hạng
        int rank = 1;
        for (TeamParticipant teamParticipant : participants) {
            teamParticipant.setRank(rank++);
        }

        // 3. Lưu toàn bộ danh sách đã có thứ hạng
        participantRepository.saveAll(participants);

        return participants;
    }

    private int resolveTopN(Round currentRound) {
        if (currentRound.getTopN() != null && currentRound.getTopN() > 0) {
            return currentRound.getTopN();
        }
        throw new BadRequestException(
                "Round chưa được cấu hình Top_N. Vui lòng cập nhật Top_N cho round trước khi thăng vòng.");
    }

    private void assertGradingComplete(Integer categoryRoundId) {
        boolean hasUngraded = evaluationRepository.existsBySubmission_TeamParticipant_CategoryRound_CategoryRoundIdAndStatus(categoryRoundId, EvaluationStatus.NOT_GRADED);
        if (hasUngraded) {
            throw new BadRequestException(
                    "Category round " + categoryRoundId + " vẫn còn bài chưa được chấm điểm (NOT_GRADED). "
                            + "Vui lòng chấm xong hết trước khi thăng vòng.");
        }
    }

    @Transactional
    public BigDecimal calculateTotalScore(TeamParticipant participant) {
        List<Evaluation> gradedEvaluations = evaluationRepository
                .findBySubmission_SubmissionIdAndStatus(participant.getId(), EvaluationStatus.GRADED);

        BigDecimal average = computeAverage(gradedEvaluations);

        participant.setTotalScore(average);
        participantRepository.save(participant);
        return average;
    }

    private BigDecimal computeAverage(List<Evaluation> gradedEvaluations) {
        if (gradedEvaluations == null || gradedEvaluations.isEmpty()) {
            return null;
        }
        BigDecimal sum = gradedEvaluations.stream()
                .map(Evaluation::getScore)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(gradedEvaluations.size()), SCORE_SCALE, RoundingMode.HALF_UP);
    }
}
