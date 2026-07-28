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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // Scheduler gọi sau khi hết thời gian đánh gia.
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

        boolean hasTeamWithoutScore = getParticipantsInRound(round).stream()
                .anyMatch(teamParticipant -> evaluationRepository
                        .findBySubmission_TeamParticipant(teamParticipant)
                        .stream()
                        .noneMatch(evaluation ->
                                (evaluation.getStatus() == EvaluationStatus.GRADED
                                        || evaluation.getStatus()
                                        == EvaluationStatus.RE_EVALUATED)
                                        && evaluation.getScore() != null
                        ));

        if (hasTeamWithoutScore) {
            throw new BadRequestException(
                    "Vẫn còn đội chưa được chấm điểm."
            );
        }

        if (isRoundFinal(round)) {
            calculateScoresAndRanking(categoryRounds.get(0).getCategoryRoundId());
        } else {
            for (CategoryRound categoryRound : categoryRounds) {
                calculateScoresAndRanking(categoryRound.getCategoryRoundId());
            }
        }

        round.setScoringProcessedAt(LocalDateTime.now());
        round.setScoringFailureNotifiedAt(null);
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
                .filter(evaluation -> evaluation.getStatus() == EvaluationStatus.GRADED || evaluation.getStatus() == EvaluationStatus.RE_EVALUATED
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

    public List<TeamParticipant> calculateRanking(
            List<TeamParticipant> participants
    ) {
        // Ưu tiên đầu tiên: đội có tổng điểm cao hơn sẽ xếp trên.
        Comparator<TeamParticipant> scoreComparator = Comparator.comparing(
                TeamParticipant::getTotalScore,
                Comparator.nullsLast(Comparator.reverseOrder())
        );

        /*
         * Gom các tiêu chí có cùng weight vào một nhóm.
         * Ví dụ: ba tiêu chí có weight 40 sẽ nằm trong cùng một List<Integer>.
         * stripTrailingZeros() giúp 40, 40.0 và 40.00 được xem là cùng weight.
         */
        Map<BigDecimal, List<Integer>> criteriaIdsByWeight =
                new HashMap<>();

        // Stream chỉ dùng để lấy danh sách tiêu chí hợp lệ.
        List<EvaluationCriteria> evaluationCriteria = participants.stream()
                .map(TeamParticipant::getCategoryRound)
                .filter(Objects::nonNull)
                .map(CategoryRound::getRound)
                .filter(Objects::nonNull)
                .map(Round::getEvaluationCriterias)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .distinct()
                .filter(criterion -> criterion.getWeight() != null)
                .toList();

        // Vòng for riêng dùng để gom các tiêu chí có cùng weight.
        for (EvaluationCriteria criterion : evaluationCriteria) {
            // Chuẩn hóa để 40, 40.0 và 40.00 thuộc cùng một nhóm.
            BigDecimal weight = criterion.getWeight()
                    .stripTrailingZeros();

            List<Integer> criteriaIds =
                    criteriaIdsByWeight.get(weight);

            // Nếu chưa có nhóm cho weight này thì tạo và lưu vào Map.
            if (criteriaIds == null) {
                criteriaIds = new ArrayList<>();
                criteriaIdsByWeight.put(weight, criteriaIds);
            }

            // Thêm ID tiêu chí hiện tại vào nhóm weight tương ứng.
            criteriaIds.add(
                    criterion.getEvaluationCriteriaId()
            );
        }

        // Sắp xếp các nhóm từ weight cao xuống thấp.
        List<List<Integer>> tieBreakCriteriaGroups =
                criteriaIdsByWeight.entrySet().stream()
                        .sorted(Map.Entry.<BigDecimal, List<Integer>>
                                comparingByKey().reversed())
                        .map(Map.Entry::getValue)
                        .toList();

        Map<Integer, Map<Integer, BigDecimal>> criteriaScoresByParticipant =
                new HashMap<>();
        for (TeamParticipant participant : participants) {
            /*
             * Tính trước điểm trung bình của từng tiêu chí cho mỗi đội.
             * Mỗi tiêu chí có thể được nhiều giám khảo chấm.
             */
            criteriaScoresByParticipant.put(
                    participant.getId(),
                    getAverageCriteriaScores(participant)
            );
        }

        /*
         * Nếu tổng điểm bằng nhau, lần lượt so sánh điểm trung bình của từng
         * nhóm weight, bắt đầu từ nhóm có weight cao nhất.
         */
        for (List<Integer> criteriaIds : tieBreakCriteriaGroups) {
            scoreComparator = scoreComparator.thenComparing(
                    participant -> getAverageWeightGroupScore(
                            criteriaScoresByParticipant.get(participant.getId()),
                            criteriaIds
                    ),
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        }

        /*
         * Thời gian nộp bài chỉ dùng để giữ thứ tự hiển thị ổn định.
         * Nó không thuộc scoreComparator nên không ảnh hưởng đến việc đồng hạng.
         */
        Comparator<TeamParticipant> displayComparator =
                scoreComparator.thenComparing(
                this::getFinalSubmissionTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        );
        participants.sort(displayComparator);

        TeamParticipant previousParticipant = null;
        int currentRank = 0;
        for (int index = 0; index < participants.size(); index++) {
            TeamParticipant participant = participants.get(index);
            /*
             * Chỉ tạo hạng mới khi tổng điểm hoặc điểm của một nhóm weight khác.
             * Nếu tất cả đều bằng nhau, đội hiện tại giữ cùng hạng với đội trước.
             */
            if (previousParticipant == null
                    || scoreComparator.compare(
                            previousParticipant,
                            participant
                    ) != 0) {
                currentRank = index + 1;
            }
            participant.setRank(currentRank);
            previousParticipant = participant;
        }

        participantRepository.saveAll(participants);
        return participants;
    }

    private BigDecimal getAverageWeightGroupScore(
            Map<Integer, BigDecimal> scoresByCriteria,
            List<Integer> criteriaIds
    ) {
        // Không thể tính điểm nhóm nếu không có dữ liệu điểm hoặc không có tiêu chí.
        if (scoresByCriteria == null
                || criteriaIds == null
                || criteriaIds.isEmpty()) {
            return null;
        }

        List<BigDecimal> scores = criteriaIds.stream()
                .map(scoresByCriteria::get)
                .filter(Objects::nonNull)
                .toList();

        /*
         * Một đội phải có điểm của tất cả tiêu chí trong nhóm.
         * Không lấy trung bình trên dữ liệu thiếu vì có thể tạo lợi thế không công bằng.
         */
        if (scores.size() != criteriaIds.size()) {
            return null;
        }

        // Điểm nhóm = tổng điểm trung bình từng tiêu chí / số tiêu chí cùng weight.
        BigDecimal total = scores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(
                BigDecimal.valueOf(scores.size()),
                SCORE_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private Map<Integer, BigDecimal> getAverageCriteriaScores(
            TeamParticipant participant
    ) {
        // Lưu toàn bộ điểm do các giám khảo chấm, được nhóm theo criteriaId.
        Map<Integer, List<BigDecimal>> scoresByCriteria = new HashMap<>();

        evaluationRepository
                .findBySubmission_TeamParticipant(participant)
                .stream()
                .filter(evaluation ->
                        evaluation.getStatus() == EvaluationStatus.GRADED
                                || evaluation.getStatus()
                                == EvaluationStatus.RE_EVALUATED
                )
                .map(Evaluation::getEvaluationDetails)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(detail -> detail.getEvaluationCriteria() != null)
                .filter(detail -> detail.getScore() != null)
                .forEach(detail -> scoresByCriteria
                        .computeIfAbsent(
                                detail.getEvaluationCriteria()
                                        .getEvaluationCriteriaId(),
                                ignored -> new ArrayList<>()
                        )
                        .add(detail.getScore())
                );

        // Từ nhiều điểm của giám khảo, tính ra một điểm trung bình cho mỗi tiêu chí.
        Map<Integer, BigDecimal> averagesByCriteria = new HashMap<>();
        scoresByCriteria.forEach((criteriaId, scores) -> {
            BigDecimal total = scores.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            averagesByCriteria.put(
                    criteriaId,
                    total.divide(
                            BigDecimal.valueOf(scores.size()),
                            SCORE_SCALE,
                            RoundingMode.HALF_UP
                    )
            );
        });
        return averagesByCriteria;
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
            Integer roundId, CustomUserDetails userDetails) {
        EventCoordinator eventCoordinator =
                userDetails.getAccount().getEventCoordinator();

        if (eventCoordinator == null) {
            throw new BadRequestException(
                    "Bạn không có quyền thực hiện. Bạn phải là event coordinator."
            );
        }

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

        if (LocalDateTime.now().isBefore(round.getResolveAppealDeadline())) {
            throw new BadRequestException(
                    "Chưa hết thời hạn giải quyết khiếu nại của Ban tổ chức, không thể thăng vòng."            );
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
