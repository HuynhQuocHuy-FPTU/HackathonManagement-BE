package com.hackathon.service.grading;

import com.hackathon.dto.CriteriaVarianceDTO;
import com.hackathon.entity.Evaluation;
import com.hackathon.entity.EvaluationCriteria;
import com.hackathon.entity.EvaluationDetail;
import com.hackathon.entity.Expert;
import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.CategoryRoundRepository;
import com.hackathon.repository.EvaluationRepository;
import com.hackathon.service.grading.support.ScoreStatisticsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GardingDashBoardService {
    private final EvaluationRepository evaluationRepository;
    private final CategoryRoundRepository categoryRoundRepository;
    private final ScoreStatisticsUtil statisticsUtil;

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
                .toList();
    }

    private CriteriaVarianceDTO buildCriteriaVarianceDTO(List<EvaluationDetail> detailsForCriteria) {
        EvaluationCriteria criteria = detailsForCriteria.get(0).getEvaluationCriteria();

        // Nhóm theo giám khảo (qua detail -> evaluation -> expertAssign -> expert)
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

        List<BigDecimal> allJudgeMeans = judgeMeans.stream()
                .map(CriteriaVarianceDTO.JudgeMeanDTO::getJudgeMean)
                .toList();

        BigDecimal overallMean = statisticsUtil.average(allJudgeMeans);
        BigDecimal stdDev = statisticsUtil.standardDeviation(allJudgeMeans, overallMean);

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
                stdDev,
                judgeMeansWithDeviation
        );
    }

    private String resolveExpertName(Expert expert) {
        return expert.getAccount() != null ? expert.getAccount().getEmail() : "Expert #" + expert.getExpertId();
    }
}
