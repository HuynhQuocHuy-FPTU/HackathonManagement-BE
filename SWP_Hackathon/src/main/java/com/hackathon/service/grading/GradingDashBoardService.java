package com.hackathon.service.grading;

import com.hackathon.dto.CriteriaVarianceDTO;
import com.hackathon.dto.EventCriteriaVarianceDTO;
import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Evaluation;
import com.hackathon.entity.EvaluationCriteria;
import com.hackathon.entity.EvaluationDetail;
import com.hackathon.entity.Expert;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.Round;
import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.CategoryRoundRepository;
import com.hackathon.repository.EvaluationRepository;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.service.grading.support.ScoreStatisticsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradingDashBoardService {
    private final EvaluationRepository evaluationRepository;
    private final CategoryRoundRepository categoryRoundRepository;
    private final ScoreStatisticsUtil statisticsUtil;
    private final HackathonEventRepository eventRepository;

    @Transactional(readOnly = true)
    public EventCriteriaVarianceDTO getEventCriteriaVarianceDashboard(Integer eventId) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sự kiện."));

        List<EventCriteriaVarianceDTO.RoundVarianceDTO> rounds = event.getRounds().stream()
                .sorted(Comparator.comparing(
                        Round::getOrderIndex,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(this::mapRoundVariance)
                .toList();

        return EventCriteriaVarianceDTO.builder()
                .eventId(event.getEventId())
                .eventName(event.getEventName())
                .rounds(rounds)
                .build();
    }

    private EventCriteriaVarianceDTO.RoundVarianceDTO mapRoundVariance(Round round) {
        List<EventCriteriaVarianceDTO.CategoryVarianceDTO> categories =
                round.getCategoryRounds().stream()
                        .sorted(Comparator.comparing(CategoryRound::getCategoryRoundId))
                        .map(categoryRound -> EventCriteriaVarianceDTO.CategoryVarianceDTO.builder()
                                .categoryRoundId(categoryRound.getCategoryRoundId())
                                .categoryId(categoryRound.getCategory().getCategoryId())
                                .categoryName(categoryRound.getCategory().getCategoryName())
                                .criteria(getCriteriaVarianceDashboard(
                                        categoryRound.getCategoryRoundId()))
                                .build())
                        .toList();

        return EventCriteriaVarianceDTO.RoundVarianceDTO.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .orderIndex(round.getOrderIndex())
                .categories(categories)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CriteriaVarianceDTO> getCriteriaVarianceDashboard(Integer categoryRoundId) {
        categoryRoundRepository.findById(categoryRoundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy category round"));

        List<Evaluation> evaluations = evaluationRepository
                .findBySubmission_TeamParticipant_CategoryRound_CategoryRoundIdAndStatus(
                        categoryRoundId, EvaluationStatus.GRADED);

        // Gom toàn bộ EvaluationDetail của mọi giám khảo, nhóm theo tiêu chí
        List<EvaluationDetail> allDetails = evaluations.stream()
                .flatMap(e -> e.getEvaluationDetails().stream())
                .toList();

        Map<Integer, List<EvaluationDetail>> byCriteria = allDetails.stream()
                .filter(d -> d.getEvaluationCriteria() != null)
                .collect(Collectors.groupingBy(d -> d.getEvaluationCriteria().getEvaluationCriteriaId()));

        return byCriteria.values().stream()
                .map(this::buildCriteriaVarianceDTO)
                .sorted(Comparator.comparing(CriteriaVarianceDTO::getEvaluationCriteriaId))
                .toList();
    }

    private CriteriaVarianceDTO buildCriteriaVarianceDTO(List<EvaluationDetail> detailsForCriteria) {
        EvaluationCriteria criteria = detailsForCriteria.get(0).getEvaluationCriteria();

        // Nhóm theo giám khảo
        Map<Expert, List<BigDecimal>> scoresByJudge = detailsForCriteria.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getEvaluation().getExpertAssign().getExpert(),
                        Collectors.mapping(EvaluationDetail::getScore, Collectors.toList())
                ));

        List<CriteriaVarianceDTO.JudgeMeanDTO> judgeMeans = scoresByJudge.entrySet().stream()
                .map(entry -> {
                    Expert expert = entry.getKey();
                    List<BigDecimal> scores = entry.getValue();
                    BigDecimal judgeMean = statisticsUtil.average(scores);
                    return new CriteriaVarianceDTO.JudgeMeanDTO(
                            expert.getExpertId(),
                            resolveExpertName(expert),
                            judgeMean,
                            null, // set deviationFromOverall sau khi có overallMean
                            scores.size()
                    );
                })
                .toList();

        List<BigDecimal> allScores = detailsForCriteria.stream()
                .map(EvaluationDetail::getScore)
                .filter(score -> score != null)
                .toList();

        BigDecimal overallMean = statisticsUtil.average(allScores);

        // So sánh các giám khảo trên cùng bài nộp.
        List<BigDecimal> itemVariances = detailsForCriteria.stream()
                .filter(detail -> detail.getScore() != null)
                .collect(Collectors.groupingBy(detail ->
                        detail.getEvaluation().getSubmission().getSubmissionId()))
                .values().stream()
                .map(details -> details.stream()
                        .map(EvaluationDetail::getScore)
                        .toList())
                .filter(scores -> scores.size() >= 2)
                .map(scores -> statisticsUtil.variance(
                        scores,
                        statisticsUtil.average(scores)
                ))
                .toList();

        BigDecimal variance = statisticsUtil.average(itemVariances);
        BigDecimal stdDev = statisticsUtil.squareRoot(variance);

        // Gán lại deviationFromOverall cho từng giám khảo sau khi đã biết overallMean
        List<CriteriaVarianceDTO.JudgeMeanDTO> judgeMeansWithDeviation = judgeMeans.stream()
                .map(jm -> new CriteriaVarianceDTO.JudgeMeanDTO(
                        jm.getExpertId(),
                        jm.getExpertName(),
                        jm.getJudgeMean(),
                        jm.getJudgeMean().subtract(overallMean),
                        jm.getSubmissionCount()
                ))
                .toList();

        return new CriteriaVarianceDTO(
                criteria.getEvaluationCriteriaId(),
                criteria.getCriteriaName(),
                overallMean,
                variance,
                stdDev,
                itemVariances.size(),
                judgeMeansWithDeviation
        );
    }

    private String resolveExpertName(Expert expert) {
        return expert.getAccount() != null ? expert.getAccount().getEmail() : "Expert #" + expert.getExpertId();
    }
}
