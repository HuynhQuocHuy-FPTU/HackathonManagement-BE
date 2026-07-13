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
    private final CategoryRoundRepository categoryRoundRepository;
    private final RoundRepository roundRepository;
    private final EvaluationRepository evaluationRepository;
    private final ParticipantRepository participantRepository;
    private final AdvancementValidator advancementValidator;
    private static final int SCORE_SCALE = 2;

    @Transactional
    public List<AdvancedTeamDTO> advanceTopTeams(Integer currentCategoryRoundId) {
        CategoryRound currentCategoryRound = categoryRoundRepository.findById(currentCategoryRoundId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy category round hiện tại."));

        Round currentRound = currentCategoryRound.getRound();

        // Chạy toàn bộ validate nghiệp vụ (thời gian chấm điểm, TopN, còn round tiếp theo không, đã cấu hình category ở round sau chưa, chấm xong chưa) TRƯỚC khi làm gì khác — tránh việc tính điểm/xếp hạng xong rồi mới phát hiện lỗi cấu hình.
        advancementValidator.validateCategoryRoundAdvancement(currentCategoryRound);

        List<TeamParticipant> teamParticipants = this.getListTeamParticipant(currentCategoryRoundId);

        //Tính totalScore cho mỗi team từ nhiều ban giám khảo cham (trung bình điểm từ các evaluation đã GRADE), sắp xếp giảm dần
        teamParticipants = this.recalculateScoresAndRanking(teamParticipants);

        int effectiveTopN = resolveTopN(currentRound);

        //tìm categoryRound tương ứng của category trước đó ở vòng tiếp theo
        CategoryRound nextCategoryRound = this.findNextCategoryRound(currentCategoryRound);

        List<AdvancedTeamDTO> result = new ArrayList<>();

        for (int i = 0; i < teamParticipants.size(); i++) {
            TeamParticipant current = teamParticipants.get(i);
            if (i < effectiveTopN) {
                current.setStatus(ParticipantStatus.PASSED);
                participantRepository.save(current);

                Registration registration = current.getRegistration();

                boolean alreadyAdvanced = participantRepository.existsByCategoryRound_CategoryRoundIdAndRegistration_RegistrationId(nextCategoryRound.getCategoryRoundId(), registration.getRegistrationId());

                if (alreadyAdvanced) {
                    log.info("Team {} đã được thăng vòng trước đó, bỏ qua", registration.getTeam().getTeamName());
                    continue;
                }
                TeamParticipant nextParticipant = TeamParticipant.builder()
                        .status(ParticipantStatus.ACTIVE)
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
    public List<CategoryAdvancementResultDTO> advanceAllCategoriesInRound(Integer roundId, CustomUserDetails userDetails) {
        EventCoordinator eventCoordinator = userDetails.getAccount().getEventCoordinator();
        if(eventCoordinator == null)
            throw new BadRequestException("Bạn không có quyền thực hiện. Bạn phải là eventcoordinator");

        Round currentRound = roundRepository.findById(roundId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy round hiện tại "));
        if(isRoundFinal(currentRound)){
            return List.of(new CategoryAdvancementResultDTO(null, selectFinalWinner(currentRound), null));
        }
        List<CategoryRound> categoryRounds = categoryRoundRepository.findCategoryRoundByRound_RoundId(roundId);

        if (categoryRounds.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy category nào thuộc round này");
        }

        List<CategoryAdvancementResultDTO> results = new ArrayList<>();

        for (CategoryRound categoryRound : categoryRounds) {
            int categoryRoundId = categoryRound.getCategoryRoundId();
                List<AdvancedTeamDTO> advanced = advanceTopTeams(categoryRoundId);
                results.add(new CategoryAdvancementResultDTO(categoryRoundId, advanced, null));
        }
        return results;
    }


    private CategoryRound findNextCategoryRound(CategoryRound currentCategoryRound) {
        Category category = currentCategoryRound.getCategory();
        Round currentRound = currentCategoryRound.getRound();

        Round nextRound = roundRepository.findRoundByHackathonEvent_EventIdAndOrderIndex(currentRound.getHackathonEvent().getEventId(), currentRound.getOrderIndex() + 1
                )
                .orElseThrow(() -> new BadRequestException("Đây đã là vòng cuối cùng, không có vòng tiếp theo"));

        return categoryRoundRepository.findCategoryRoundByCategory_CategoryIdAndRound_RoundId(category.getCategoryId(), nextRound.getRoundId()).orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa cấu hình category này cho vòng tiếp theo (thiếu CategoryRound)"));
    }


    @Transactional
    public void disqualifyRetroactively(TeamParticipant oldTeamParticipant, Round nextRound) {
        CategoryRound categoryRound = oldTeamParticipant.getCategoryRound();
        Category category = categoryRound.getCategory();

        List<TeamParticipant> teamParticipantFailed = new ArrayList<>(
                categoryRound.getTeamParticipants().stream()
                        .filter(p -> p.getStatus() == ParticipantStatus.FAILED)
                        .toList()
        );
        teamParticipantFailed.sort(Comparator.comparing(TeamParticipant::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder())));

        if (teamParticipantFailed.isEmpty()) {
            log.warn("Không có team FAILED nào để đôn thay thế cho categoryRound {}", categoryRound.getCategoryRoundId());
        } else {
            TeamParticipant replaceTeam = teamParticipantFailed.getFirst();
            replaceTeam.setStatus(ParticipantStatus.PASSED);
            participantRepository.save(replaceTeam);

            Registration registration = replaceTeam.getRegistration();

            CategoryRound nextCategoryRound = categoryRoundRepository
                    .findCategoryRoundByCategory_CategoryIdAndRound_RoundId(category.getCategoryId(), nextRound.getRoundId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Chưa cấu hình category này cho vòng tiếp theo (thiếu CategoryRound)"));

            boolean alreadyAdvanced = participantRepository.existsByCategoryRound_CategoryRoundIdAndRegistration_RegistrationId(
                    nextCategoryRound.getCategoryRoundId(), registration.getRegistrationId());

            if (!alreadyAdvanced) {
                TeamParticipant nextParticipant = TeamParticipant.builder()
                        .status(ParticipantStatus.ACTIVE)
                        .categoryRound(nextCategoryRound)
                        .registration(registration)
                        .build();
                participantRepository.save(nextParticipant);
            }

            log.info("Đội {} đã được đôn lên thay thế.", registration.getTeam().getTeamName());
        }

        // Cập nhật lại Ranking cho toàn bộ CategoryRound đó — sau khi loại 1 đội và
        // (có thể) đôn 1 đội, thứ hạng hiện tại đã thay đổi
        calculateRanking(
                participantRepository.findByCategoryRound_CategoryRoundIdAndStatusIsNotIn(
                        categoryRound.getCategoryRoundId(),
                        List.of(ParticipantStatus.DISQUALIFIED, ParticipantStatus.WITHDRAWN)
                )
        );
    }

    private List<TeamParticipant> recalculateScoresAndRanking(List<TeamParticipant> participants) {
        if(participants.isEmpty()){
            throw new BadRequestException("Chưa có đội tham gia");
        }

        // 2. Tính toán điểm số
        for (TeamParticipant participant : participants) {
            this.calculateTotalScore(participant);
        }

        // Lưu các thay đổi về điểm số (nếu có)
        participantRepository.saveAll(participants);

        // 3. Gọi hàm xếp hạng sau khi đã có điểm
        return calculateRanking(participants);
    }
    private List<TeamParticipant>  getListTeamParticipant(Integer categoryRoundId){
        // 1. Lấy danh sách hợp lệ
        List<TeamParticipant> participants = participantRepository.findByCategoryRound_CategoryRoundIdAndStatusIsNotIn(
                categoryRoundId,
                List.of(ParticipantStatus.DISQUALIFIED, ParticipantStatus.WITHDRAWN)
        );
        if (participants.isEmpty()) {
            throw new BadRequestException("Chưa có đội tham gia");
        }

        return participants;
    }

    private List<TeamParticipant> calculateRanking(List<TeamParticipant> participants) {
        // 1. Sắp xếp theo điểm số (giảm dần)
        participants.sort(Comparator.comparing(
                TeamParticipant::getTotalScore,
                Comparator.nullsLast(Comparator.reverseOrder())
        ).thenComparing(t -> this.getFinalSubmissionTime(t), Comparator.nullsLast(Comparator.naturalOrder())));
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


    @Transactional
    public BigDecimal calculateTotalScore(TeamParticipant participant) {
        List<Evaluation> gradedEvaluations = evaluationRepository.findBySubmission_TeamParticipant(participant).stream().filter(e -> e.getStatus() == EvaluationStatus.GRADED).toList();

        BigDecimal average = computeAverage(gradedEvaluations);

        participant.setTotalScore(average);
        return average;
    }

    private BigDecimal computeAverage(List<Evaluation> gradedEvaluations) {
        if (gradedEvaluations == null || gradedEvaluations.isEmpty()) {
            return null;
        }
        BigDecimal sum = gradedEvaluations.stream()
                .map(Evaluation::getScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(gradedEvaluations.size()), SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private boolean isRoundFinal(Round round){
        return roundRepository.findRoundByHackathonEvent_EventIdAndOrderIndex(round.getHackathonEvent().getEventId(), round.getOrderIndex() + 1).isEmpty();
    }

    private List<AdvancedTeamDTO> selectFinalWinner(Round finalRound){

        List<TeamParticipant> teamParticipants = new ArrayList<>();
        //lấy tất cả team tham gia trong round cuối
        for(CategoryRound cr: finalRound.getCategoryRounds()){
            teamParticipants.addAll(this.getListTeamParticipant(cr.getCategoryRoundId()));
        }
        teamParticipants = this.recalculateScoresAndRanking(teamParticipants);

        int topN = resolveTopN(finalRound);

        for (TeamParticipant participant : teamParticipants) {

            participant.setStatus(
                    participant.getRank() <= topN
                            ? ParticipantStatus.PASSED
                            : ParticipantStatus.FAILED
            );
        }
        participantRepository.saveAll(teamParticipants);

        return teamParticipants.stream().map(t -> this.mapTo(t, null)).toList();
    }

    private AdvancedTeamDTO mapTo(TeamParticipant teamParticipant, Integer newTeamParticipantId){
        Team team = teamParticipant.getRegistration().getTeam();
        return new AdvancedTeamDTO(team.getTeamId(), team.getTeamName(), teamParticipant.getTotalScore(), newTeamParticipantId );
    }

    public LocalDateTime getFinalSubmissionTime(TeamParticipant teamParticipant) {
        return teamParticipant.getSubmissions().stream()
                .filter(Submission::isFinal) // Lọc bài nộp cuối cùng
                .map(Submission::getCreateAt) // thời gian nộp bài
                .findFirst()
                .orElse(null);
    }

}
