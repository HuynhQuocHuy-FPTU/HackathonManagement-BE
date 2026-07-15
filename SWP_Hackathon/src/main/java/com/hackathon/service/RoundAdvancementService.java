package com.hackathon.service;

import com.hackathon.dto.team.AdvancedTeamDTO;
import com.hackathon.dto.team.CategoryAdvancementResultDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.CategoryRoundRepository;
import com.hackathon.repository.EvaluationRepository;
import com.hackathon.repository.ParticipantRepository;
import com.hackathon.repository.RoundRepository;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.validator.AdvancementValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoundAdvancementService {

    private static final int SCORE_SCALE = 2;

    private final CategoryRoundRepository categoryRoundRepository;
    private final RoundRepository roundRepository;
    private final EvaluationRepository evaluationRepository;
    private final ParticipantRepository participantRepository;
    private final AdvancementValidator advancementValidator;
    //tính điểm và ranking
    @Transactional
    public List<AdvancedTeamDTO> calculateScoresAndRanking(Integer categoryRoundId) {
        CategoryRound categoryRound = categoryRoundRepository.findById(categoryRoundId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy category round hiện tại."));

        Round currentRound = categoryRound.getRound();

        List<TeamParticipant> participants = isRoundFinal(currentRound)
                ? getParticipantsInRound(currentRound)
                : getListTeamParticipant(categoryRound.getCategoryRoundId());

        return recalculateScoresAndRanking(participants).stream()
                .map(participant -> mapTo(participant, null))
                .toList();
    }

    // Scheduler gọi sau khi hết thời gian nộp bài.
    @Transactional
    public void calculateRoundScoresAutomatically(Integer roundId) {
        Round round = roundRepository.findByIdForAdvancement(roundId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy round hiện tại."));

        if (round.getScoringProcessedAt() != null) {
            return;
        }

        List<CategoryRound> categoryRounds = round.getCategoryRounds();
        if (categoryRounds == null || categoryRounds.isEmpty()) {
            throw new BadRequestException("Round chưa có category nào.");
        }

        if (isRoundFinal(round)) {
            calculateScoresAndRanking(categoryRounds.get(0).getCategoryRoundId());
        } else {
            for (CategoryRound categoryRound : categoryRounds) {
                calculateScoresAndRanking(categoryRound.getCategoryRoundId());
            }
        }

        boolean hasTeamWithoutScore = getParticipantsInRound(round).stream()
                .anyMatch(participant -> participant.getTotalScore() == null);

        if (hasTeamWithoutScore) {
            throw new BadRequestException(
                    "Vẫn còn đội chưa có điểm, hệ thống sẽ thử tính lại sau."
            );
        }

        round.setScoringProcessedAt(LocalDateTime.now());
        roundRepository.save(round);
    }

    private List<TeamParticipant> recalculateScoresAndRanking(
            List<TeamParticipant> participants
    ) {
        if (participants.isEmpty()) {
            throw new BadRequestException("Chưa có đội tham gia.");
        }

        participants.forEach(this::calculateTotalScore);
        participantRepository.saveAll(participants);

        return calculateRanking(participants);
    }

    @Transactional
    public BigDecimal calculateTotalScore(TeamParticipant participant) {
        List<Evaluation> gradedEvaluations = evaluationRepository
                .findBySubmission_TeamParticipant(participant)
                .stream()
                .filter(evaluation -> evaluation.getStatus() == EvaluationStatus.GRADED
                        || evaluation.getStatus() == EvaluationStatus.RE_EVALUATED
                )
                .toList();

        BigDecimal average = computeAverage(gradedEvaluations);
        participant.setTotalScore(average);
        return average;
    }

    private BigDecimal computeAverage(List<Evaluation> gradedEvaluations) {
        if (gradedEvaluations == null || gradedEvaluations.isEmpty()) {
            return null;
        }

        List<BigDecimal> validScores = gradedEvaluations.stream()
                .map(Evaluation::getScore)
                .filter(Objects::nonNull)
                .toList();

        if (validScores.isEmpty()) {
            return null;
        }

        BigDecimal sum = validScores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(
                BigDecimal.valueOf(validScores.size()),
                SCORE_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private List<TeamParticipant> calculateRanking(
            List<TeamParticipant> participants
    ) {
        participants.sort(
                Comparator.comparing(
                                TeamParticipant::getTotalScore,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(
                                this::getFinalSubmissionTime,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
        );

        int rank = 1;
        for (TeamParticipant participant : participants) {
            participant.setRank(rank++);
        }

        participantRepository.saveAll(participants);
        return participants;
    }

    //thăng vòng theo từng
    @Transactional
    public List<AdvancedTeamDTO> advanceTopTeams(Integer currentCategoryRoundId) {
        CategoryRound currentCategoryRound = categoryRoundRepository
                .findById(currentCategoryRoundId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy category round hiện tại."));

        Round currentRound = currentCategoryRound.getRound();

        if (isRoundFinal(currentRound)) {
            return selectFinalWinners(currentRound);
        }

        advancementValidator.validateCategoryRoundAdvancement(currentCategoryRound);

        List<TeamParticipant> participants =
                getListTeamParticipant(currentCategoryRoundId);
        validateRankingCalculated(participants);
        sortByRank(participants);

        int topN = resolveTopN(currentRound);
        CategoryRound nextCategoryRound = findNextCategoryRound(currentCategoryRound);

        return advanceToNextRound(participants, nextCategoryRound, topN);
    }

    private List<AdvancedTeamDTO> advanceToNextRound(
            List<TeamParticipant> participants,
            CategoryRound nextCategoryRound,
            int topN
    ) {
        List<AdvancedTeamDTO> result = new ArrayList<>();

        for (TeamParticipant participant : participants) {
            boolean passed = participant.getRank() <= topN;
            participant.setStatus(
                    passed ? ParticipantStatus.PASSED : ParticipantStatus.FAILED
            );

            if (!passed) {
                continue;
            }

            Registration registration = participant.getRegistration();
            boolean alreadyAdvanced = participantRepository
                    .existsByCategoryRound_CategoryRoundIdAndRegistration_RegistrationId(
                            nextCategoryRound.getCategoryRoundId(),
                            registration.getRegistrationId()
                    );

            if (alreadyAdvanced) {
                log.info(
                        "Team {} đã được thăng vòng trước đó, bỏ qua.",
                        registration.getTeam().getTeamName()
                );
                continue;
            }

            TeamParticipant nextParticipant = TeamParticipant.builder()
                    .status(ParticipantStatus.ACTIVE)
                    .categoryRound(nextCategoryRound)
                    .registration(registration)
                    .build();

            TeamParticipant savedParticipant =
                    participantRepository.save(nextParticipant);

            result.add(mapTo(participant, savedParticipant.getId()));
        }

        participantRepository.saveAll(participants);
        return result;
    }

    // thăng vòng cho vòng cuối
    private List<AdvancedTeamDTO> selectFinalWinners(Round finalRound) {
        List<TeamParticipant> participants = getParticipantsInRound(finalRound);
        validateRankingCalculated(participants);
        sortByRank(participants);

        int topN = resolveTopN(finalRound);

        for (TeamParticipant participant : participants) {
            participant.setStatus(
                    participant.getRank() <= topN
                            ? ParticipantStatus.PASSED
                            : ParticipantStatus.FAILED
            );
        }

        participantRepository.saveAll(participants);

        return participants.stream()
                .filter(participant -> participant.getStatus() == ParticipantStatus.PASSED)
                .map(participant -> mapTo(participant, null))
                .toList();
    }
    //thăng vòng
    @Transactional
    public List<CategoryAdvancementResultDTO> advanceAllCategoriesInRound(
            Integer roundId) {
//        EventCoordinator eventCoordinator =
//                userDetails.getAccount().getEventCoordinator();
//
//        if (eventCoordinator == null) {
//            throw new BadRequestException(
//                    "Bạn không có quyền thực hiện. Bạn phải là event coordinator."
//            );
//        }

        return processRoundAdvancement(roundId);
    }

    // Được scheduler gọi khi đã hết thời gian chờ thăng vòng.
    @Transactional
    public List<CategoryAdvancementResultDTO> advanceRoundAutomatically(Integer roundId) {
        return processRoundAdvancement(roundId);
    }

    private List<CategoryAdvancementResultDTO> processRoundAdvancement(Integer roundId) {
        Round currentRound = roundRepository.findByIdForAdvancement(roundId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy round hiện tại."));

        validateAppealFinished(currentRound);

        // API hoặc scheduler đã xử lý trước đó.
        if (currentRound.getAdvancementProcessedAt() != null) {
            return List.of();
        }

        if (isRoundFinal(currentRound)) {
            List<CategoryAdvancementResultDTO> result = List.of(new CategoryAdvancementResultDTO(
                    null,
                    selectFinalWinners(currentRound),
                    null
            ));
            markAdvancementProcessed(currentRound);
            return result;
        }

        List<CategoryRound> categoryRounds = categoryRoundRepository
                .findCategoryRoundByRound_RoundId(roundId);

        if (categoryRounds.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy category nào thuộc round này."
            );
        }

        List<CategoryAdvancementResultDTO> results = new ArrayList<>();

        for (CategoryRound categoryRound : categoryRounds) {
            int categoryRoundId = categoryRound.getCategoryRoundId();
            List<AdvancedTeamDTO> advanced = advanceTopTeams(categoryRoundId);

            results.add(new CategoryAdvancementResultDTO(
                    categoryRoundId,
                    advanced,
                    null
            ));
        }

        markAdvancementProcessed(currentRound);
        return results;
    }

    private void markAdvancementProcessed(Round round) {
        round.setAdvancementProcessedAt(LocalDateTime.now());
        roundRepository.save(round);
    }

    private void validateAppealFinished(Round round) {
        if (round.getAppealEndTime() == null) {
            throw new BadRequestException(
                    "Vòng thi chưa cấu hình thời gian kết thúc khiếu nại"
            );
        }

//        if (round.getStatus() != RoundStatus.FINAL_RESULT) {
//            throw new BadRequestException(
//                    "Chưa kết thúc thời gian khiếu nại, không thể thăng vòng"
//            );
//        }
        if (LocalDateTime.now().isBefore(round.getResolveAppealDeadline())) {
            throw new BadRequestException(
                    "Thời gian nộp đơn khiếu nại của thí sinh vẫn chưa kết thúc, không thể thăng vòng!"
            );
        }
    }

    private void validateRankingCalculated(List<TeamParticipant> participants) {
        boolean notCalculated = participants.stream()
                .anyMatch(participant ->
                        participant.getTotalScore() == null
                                || participant.getRank() == null
                );

        if (notCalculated) {
            throw new BadRequestException(
                    "Chưa tính điểm hoặc xếp hạng. "
                            + "Vui lòng tính điểm trước khi thực hiện thăng vòng."
            );
        }
    }

    private void sortByRank(List<TeamParticipant> participants) {
        participants.sort(Comparator.comparing(
                TeamParticipant::getRank,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
    }

    private CategoryRound findNextCategoryRound(
            CategoryRound currentCategoryRound
    ) {
        Category category = currentCategoryRound.getCategory();
        Round currentRound = currentCategoryRound.getRound();

        Round nextRound = roundRepository
                .findRoundByHackathonEvent_EventIdAndOrderIndex(
                        currentRound.getHackathonEvent().getEventId(),
                        currentRound.getOrderIndex() + 1
                )
                .orElseThrow(() -> new BadRequestException(
                        "Đây đã là vòng cuối cùng, không có vòng tiếp theo."
                ));

        return categoryRoundRepository
                .findCategoryRoundByCategory_CategoryIdAndRound_RoundId(
                        category.getCategoryId(),
                        nextRound.getRoundId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa cấu hình category này cho vòng tiếp theo "
                                + "(thiếu CategoryRound)."
                ));
    }

    //Loại đội bổ sung
    @Transactional
    public void disqualifyRetroactively(
            TeamParticipant oldTeamParticipant,
            Round nextRound
    ) {
        CategoryRound categoryRound = oldTeamParticipant.getCategoryRound();
        Category category = categoryRound.getCategory();

        List<TeamParticipant> failedParticipants = new ArrayList<>(
                categoryRound.getTeamParticipants().stream()
                        .filter(participant ->
                                participant.getStatus() == ParticipantStatus.FAILED
                        )
                        .toList()
        );

        List<TeamParticipant> participants =
                getListTeamParticipant(categoryRound.getCategoryRoundId());
        participants.remove(oldTeamParticipant);

        failedParticipants.sort(Comparator.comparing(
                TeamParticipant::getTotalScore,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        if (failedParticipants.isEmpty()) {
            log.warn(
                    "Không có team FAILED nào để đôn thay thế cho categoryRound {}.",
                    categoryRound.getCategoryRoundId()
            );
        } else {
            TeamParticipant replacement = failedParticipants.get(0);
            replacement.setStatus(ParticipantStatus.PASSED);
            participantRepository.save(replacement);

            Registration registration = replacement.getRegistration();

            CategoryRound nextCategoryRound = categoryRoundRepository
                    .findCategoryRoundByCategory_CategoryIdAndRound_RoundId(
                            category.getCategoryId(),
                            nextRound.getRoundId()
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Chưa cấu hình category này cho vòng tiếp theo "
                                    + "(thiếu CategoryRound)."
                    ));

            boolean alreadyAdvanced = participantRepository
                    .existsByCategoryRound_CategoryRoundIdAndRegistration_RegistrationId(
                            nextCategoryRound.getCategoryRoundId(),
                            registration.getRegistrationId()
                    );

            if (!alreadyAdvanced) {
                TeamParticipant nextParticipant = TeamParticipant.builder()
                        .status(ParticipantStatus.ACTIVE)
                        .categoryRound(nextCategoryRound)
                        .registration(registration)
                        .build();
                participantRepository.save(nextParticipant);
            }

            log.info(
                    "Đội {} đã được đôn lên thay thế.",
                    registration.getTeam().getTeamName()
            );
        }

        calculateRanking(participants);
    }

//---------------------------------------------

    private List<TeamParticipant> getListTeamParticipant(
            Integer categoryRoundId
    ) {
        List<TeamParticipant> participants = participantRepository
                .findByCategoryRound_CategoryRoundIdAndStatusIsNotIn(
                        categoryRoundId,
                        List.of(ParticipantStatus.DISQUALIFIED,
                                ParticipantStatus.WITHDRAWN));

        if (participants.isEmpty()) {
            throw new BadRequestException("Chưa có đội tham gia.");
        }

        return participants;
    }

    private List<TeamParticipant> getParticipantsInRound(Round round) {
        List<TeamParticipant> participants = new ArrayList<>();

        for (CategoryRound categoryRound : round.getCategoryRounds()) {
            participants.addAll(
                    getListTeamParticipant(categoryRound.getCategoryRoundId())
            );
        }

        if (participants.isEmpty()) {
            throw new BadRequestException("Chưa có đội tham gia trong round.");
        }

        return participants;
    }

    private int resolveTopN(Round currentRound) {
        if (currentRound.getTopN() != null && currentRound.getTopN() > 0) {
            return currentRound.getTopN();
        }

        throw new BadRequestException(
                "Round chưa được cấu hình Top_N. "
                        + "Vui lòng cập nhật Top_N trước khi thăng vòng."
        );
    }

    private boolean isRoundFinal(Round round) {
        return roundRepository
                .findRoundByHackathonEvent_EventIdAndOrderIndex(
                        round.getHackathonEvent().getEventId(),
                        round.getOrderIndex() + 1
                )
                .isEmpty();
    }

    private AdvancedTeamDTO mapTo(
            TeamParticipant participant,
            Integer newTeamParticipantId
    ) {
        Team team = participant.getRegistration().getTeam();

        return new AdvancedTeamDTO(
                team.getTeamId(),
                team.getTeamName(),
                participant.getTotalScore(),
                participant.getRank(),
                newTeamParticipantId
        );
    }

    public LocalDateTime getFinalSubmissionTime(
            TeamParticipant participant
    ) {
        return participant.getSubmissions().stream()
                .filter(Submission::isFinal)
                .map(Submission::getCreateAt)
                .findFirst()
                .orElse(null);
    }
}
