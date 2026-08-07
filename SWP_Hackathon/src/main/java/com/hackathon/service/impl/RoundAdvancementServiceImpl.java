package com.hackathon.service.impl;

import com.hackathon.dto.team.AdvancedTeamDTO;
import com.hackathon.dto.team.CategoryAdvancementResultDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.entity.enums.ExpertRole;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.CategoryRoundRepository;
import com.hackathon.repository.EvaluationRepository;
import com.hackathon.repository.ParticipantRepository;
import com.hackathon.repository.RoundRepository;
import com.hackathon.repository.SubmissionRepository;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.RoundAdvancementService;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
// Tính điểm, xếp hạng và chuyển các đội đủ điều kiện sang vòng thi kế tiếp.
public class RoundAdvancementServiceImpl implements RoundAdvancementService {

    private static final int SCORE_SCALE = 2;

    private final CategoryRoundRepository categoryRoundRepository;
    private final RoundRepository roundRepository;
    private final EvaluationRepository evaluationRepository;
    private final ParticipantRepository participantRepository;
    private final SubmissionRepository submissionRepository;
    private final AdvancementValidator advancementValidator;
    // Tính lại tổng điểm và thứ hạng cho các đội trong một danh mục vòng.
    @Transactional
    @Override
    public List<AdvancedTeamDTO> calculateScoresAndRanking(Integer categoryRoundId) {
        // Tìm danh mục vòng cần xử lý và lấy vòng thi tương ứng.
        CategoryRound categoryRound = categoryRoundRepository.findById(categoryRoundId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy category round hiện tại."));

        Round currentRound = categoryRound.getRound();

        // Vòng cuối xếp hạng chung toàn vòng, các vòng trước xếp riêng theo danh mục.
        List<TeamParticipant> participants = isRoundFinal(currentRound)
                ? getParticipantsInRound(currentRound)
                : getListTeamParticipant(categoryRound.getCategoryRoundId());

        // Không tính kết quả khi còn giám khảo được phân công chưa hoàn tất đánh giá.
        validateAllAssignedJudgesHaveEvaluated(participants);

        return recalculateScoresAndRanking(participants).stream()
                .map(participant -> mapTo(participant, null))
                .toList();
    }

    // Tác vụ nền tính điểm cho toàn bộ vòng sau khi hết thời gian đánh giá.
    @Transactional
    @Override
    public void calculateRoundScoresAutomatically(Integer roundId) {
        // Tải vòng cùng dữ liệu phục vụ thăng vòng và khóa xử lý lặp.
        Round round = roundRepository.findByIdForAdvancement(roundId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy round hiện tại."));

        // Bỏ qua khi vòng đã được tính điểm thành công trước đó.
        if (round.getScoringProcessedAt() != null) {
            return;
        }

        // Vòng phải có ít nhất một danh mục để xác định các đội cần tính.
        List<CategoryRound> categoryRounds = round.getCategoryRounds();
        if (categoryRounds == null || categoryRounds.isEmpty()) {
            throw new BadRequestException("Round chưa có category nào.");
        }

        // Vòng cuối tính chung một lần, các vòng khác tính riêng từng danh mục.
        if (isRoundFinal(round)) {
            calculateScoresAndRanking(categoryRounds.get(0).getCategoryRoundId());
        } else {
            for (CategoryRound categoryRound : categoryRounds) {
                calculateScoresAndRanking(categoryRound.getCategoryRoundId());
            }
        }

        // Ghi nhận thời điểm hoàn tất và xóa dấu vết cảnh báo thất bại cũ.
        round.setScoringProcessedAt(LocalDateTime.now());
        round.setScoringFailureNotifiedAt(null);
        roundRepository.save(round);
    }

    // Tính lại điểm từng đội, lưu kết quả rồi sắp xếp thứ hạng.
    private List<TeamParticipant> recalculateScoresAndRanking(
            List<TeamParticipant> participants
    ) {
        // Không thể xếp hạng khi danh sách chưa có đội tham gia.
        if (participants.isEmpty()) {
            throw new BadRequestException("Chưa có đội tham gia.");
        }

        // Tính tổng điểm cho từng đội trước khi lưu đồng loạt.
        participants.forEach(this::calculateTotalScore);
        participantRepository.saveAll(participants);

        return calculateRanking(participants);
    }

    @Transactional
    @Override
    // Tính điểm trung bình từ các lượt đánh giá hợp lệ của một đội.
    public BigDecimal calculateTotalScore(TeamParticipant participant) {
        // Chỉ lấy đánh giá đã hoàn tất chấm lần đầu hoặc hoàn tất chấm lại.
        List<Evaluation> gradedEvaluations = evaluationRepository
                .findBySubmission_TeamParticipant(participant)
                .stream()
                .filter(evaluation -> evaluation.getStatus() == EvaluationStatus.GRADED || evaluation.getStatus() == EvaluationStatus.RE_EVALUATED
                )
                .toList();

        // Ghi điểm trung bình vào lần tham gia để phục vụ xếp hạng.
        BigDecimal average = computeAverage(gradedEvaluations);
        participant.setTotalScore(average);
        return average;
    }

    // Tính trung bình cộng của các tổng điểm hợp lệ và làm tròn theo độ chính xác chung.
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

    @Override
    public List<TeamParticipant> calculateRanking(
            List<TeamParticipant> participants
    ) {
        // Ưu tiên đầu tiên: đội có tổng điểm cao hơn sẽ xếp trên.
        Comparator<TeamParticipant> scoreComparator = Comparator.comparing(
                TeamParticipant::getTotalScore,
                Comparator.nullsLast(Comparator.reverseOrder())
        );

        // Gom các tiêu chí có cùng weight vào một nhóm.
        // Ví dụ: ba tiêu chí có weight 40 sẽ nằm trong cùng một List<Integer>.
        // stripTrailingZeros() giúp 40, 40.0 và 40.00 được xem là cùng weight.
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
            // Tính trước điểm trung bình của từng tiêu chí cho mỗi đội.
            // Mỗi tiêu chí có thể được nhiều giám khảo chấm.
            criteriaScoresByParticipant.put(
                    participant.getId(),
                    getAverageCriteriaScores(participant)
            );
        }

        // Nếu tổng điểm bằng nhau, lần lượt so sánh điểm trung bình của từng
        // nhóm weight, bắt đầu từ nhóm có weight cao nhất.
        for (List<Integer> criteriaIds : tieBreakCriteriaGroups) {
            scoreComparator = scoreComparator.thenComparing(
                    participant -> getAverageWeightGroupScore(
                            criteriaScoresByParticipant.get(participant.getId()),
                            criteriaIds
                    ),
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        }

        // Thời gian nộp bài chỉ dùng để giữ thứ tự hiển thị ổn định.
        // Nó không thuộc scoreComparator nên không ảnh hưởng đến việc đồng hạng.
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
            // Chỉ tạo hạng mới khi tổng điểm hoặc điểm của một nhóm weight khác.
            // Nếu tất cả đều bằng nhau, đội hiện tại giữ cùng hạng với đội trước.
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

    private BigDecimal getAverageWeightGroupScore(Map<Integer, BigDecimal> scoresByCriteria, List<Integer> criteriaIds
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

        // Một đội phải có điểm của tất cả tiêu chí trong nhóm.
        // Không lấy trung bình trên dữ liệu thiếu vì có thể tạo lợi thế không công bằng.
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
    @Override
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

    // Đánh dấu kết quả đạt hoặc không đạt và tạo lần tham gia vòng sau cho các đội đứng đầu.
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
            boolean alreadyAdvanced = participantRepository.existsByCategoryRound_CategoryRoundIdAndRegistration_RegistrationId(nextCategoryRound.getCategoryRoundId(), registration.getRegistrationId());

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
    @Override
    public List<CategoryAdvancementResultDTO> advanceAllCategoriesInRound(
            Integer roundId, CustomUserDetails userDetails) {
        EventCoordinator eventCoordinator =
                userDetails.getAccount().getEventCoordinator();

        if (eventCoordinator == null) {
            throw new BadRequestException(
                    "Bạn không có quyền thực hiện. Bạn phải là event coordinator."
            );
        }

        return processRoundAdvancement(roundId, true);
    }

    // Được scheduler gọi khi đã hết thời gian chờ thăng vòng.
    @Transactional
    @Override
    public List<CategoryAdvancementResultDTO> advanceRoundAutomatically(Integer roundId) {
        return processRoundAdvancement(roundId, false);
    }

    // Dùng chung quy trình thăng vòng cho thao tác thủ công và tác vụ tự động.
    private List<CategoryAdvancementResultDTO> processRoundAdvancement(
            Integer roundId,
            boolean validateAppeal
    ) {
        Round currentRound = roundRepository.findByIdForAdvancement(roundId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy round hiện tại."));

        if (validateAppeal) {
            validateAppealFinished(currentRound);
        }

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

    // Ghi thời điểm hoàn tất để ngăn cùng một vòng được xử lý thăng hạng nhiều lần.
    private void markAdvancementProcessed(Round round) {
        round.setAdvancementProcessedAt(LocalDateTime.now());
        roundRepository.save(round);
    }

    // Chặn thăng vòng thủ công khi ban tổ chức vẫn còn thời gian giải quyết khiếu nại.
    private void validateAppealFinished(Round round) {
        if (round.getAppealEndTime() == null) {
            throw new BadRequestException(
                    "Vòng thi chưa cấu hình thời gian kết thúc khiếu nại"
            );
        }

        if (LocalDateTime.now().isBefore(round.getResolveAppealDeadline())) {
            throw new BadRequestException(
                    "Chưa hết thời hạn giải quyết khiếu nại của Ban tổ chức, không thể thăng vòng."        );
        }
    }

    // Xác nhận tất cả đội đã có tổng điểm và thứ hạng trước khi xét thăng vòng.
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

    // Sắp xếp đội theo thứ hạng tăng dần và đưa đội chưa có hạng xuống cuối.
    private void sortByRank(List<TeamParticipant> participants) {
        participants.sort(Comparator.comparing(
                TeamParticipant::getRank,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
    }

    // Tìm danh mục tương ứng của vòng kế tiếp trong cùng sự kiện.
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
                        "Chưa cấu hình category này cho vòng tiếp theo " + "(thiếu CategoryRound)."
                ));
    }

    //Loại đội bổ sung
    @Transactional
    @Override
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

    // Gom các đội hợp lệ từ tất cả danh mục thuộc một vòng thi.
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

    // Kiểm tra từng đội đã được tất cả giám khảo phân công hoàn tất chấm bài.
    private void validateAllAssignedJudgesHaveEvaluated(
            List<TeamParticipant> participants
    ) {
        List<String> missingEvaluationMessages = new ArrayList<>();

        for (TeamParticipant participant : participants) {
            CategoryRound categoryRound = participant.getCategoryRound();

            List<ExpertAssign> assignedJudges = categoryRound.getExpertAssigns().stream()
                    .filter(assignment -> assignment.getRole() == ExpertRole.CORE_JUDGE
                            || assignment.getRole() == ExpertRole.GUEST_JUDGE)
                    .toList();

            if (assignedJudges.isEmpty()) {
                throw new BadRequestException(
                        "Category \"" + categoryRound.getCategory().getCategoryName()
                                + "\" chưa được phân công giám khảo."
                );
            }

            Submission finalSubmission = findFinalSubmission(participant);

            Set<Integer> completedAssignmentIds = evaluationRepository
                    .findBySubmission_SubmissionId(finalSubmission.getSubmissionId())
                    .stream()
                    .filter(this::isValidEvaluation)
                    .map(Evaluation::getExpertAssign)
                    .filter(Objects::nonNull)
                    .map(ExpertAssign::getAssignId)
                    .collect(Collectors.toSet());

            List<ExpertAssign> missingJudges = assignedJudges.stream()
                    .filter(assignment -> !completedAssignmentIds.contains(assignment.getAssignId()))
                    .toList();

            if (!missingJudges.isEmpty()) {
                String teamName = participant.getRegistration().getTeam().getTeamName();
                int completedCount = assignedJudges.size() - missingJudges.size();
                String missingJudgeNames = missingJudges.stream()
                        .map(assignment -> assignment.getExpert().getExpertName()
                                + " (" + assignment.getRole() + ")")
                        .collect(Collectors.joining(", "));

                missingEvaluationMessages.add(
                        "Team " + teamName + " mới hoàn thành " + completedCount
                                + "/" + assignedJudges.size()
                                + " đánh giá. Còn thiếu: " + missingJudgeNames
                );
            }
        }

        if (!missingEvaluationMessages.isEmpty()) {
            throw new BadRequestException(
                    "Không thể tính điểm vì chưa đủ đánh giá. "
                            + String.join(" | ", missingEvaluationMessages)
            );
        }
    }

    // Tìm bài chính thức mới nhất của đội để kiểm tra kết quả chấm.
    private Submission findFinalSubmission(TeamParticipant participant) {
        return submissionRepository
                .findByTeamParticipant_IdAndIsFinalTrue(participant.getId())
                .stream()
                .max(Comparator.comparing(Submission::getCreateAt))
                .orElseThrow(() -> new BadRequestException(
                        "Team " + participant.getRegistration().getTeam().getTeamName()
                                + " chưa có final submission."
                ));
    }

    // Xác định lượt đánh giá đã hoàn tất và có tổng điểm hợp lệ.
    private boolean isValidEvaluation(Evaluation evaluation) {
        if (evaluation == null) {
            return false;
        }

        boolean validStatus = evaluation.getStatus() == EvaluationStatus.GRADED
                || evaluation.getStatus() == EvaluationStatus.RE_EVALUATED;

        return validStatus && evaluation.getScore() != null;
    }

    // Lấy số đội được chọn từ cấu hình vòng và từ chối cấu hình không hợp lệ.
    private int resolveTopN(Round currentRound) {
        if (currentRound.getTopN() != null && currentRound.getTopN() > 0) {
            return currentRound.getTopN();
        }

        throw new BadRequestException(
                "Round chưa được cấu hình Top_N. "
                        + "Vui lòng cập nhật Top_N trước khi thăng vòng."
        );
    }

    // Xác định vòng cuối bằng cách kiểm tra không còn vòng có thứ tự kế tiếp.
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

    @Override
    // Lấy thời điểm nộp bài chính thức để giữ thứ tự hiển thị ổn định khi đồng hạng.
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
