package com.hackathon.service;

import com.hackathon.dto.evaluation.EvaluationDetailResponse;
import com.hackathon.dto.evaluation.EvaluationResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.RequestStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.EvaluationDetailRepository;
import com.hackathon.repository.EvaluationRepository;
import com.hackathon.repository.SubmissionRepository;
import com.hackathon.repository.TeamRequestRepository;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationDetailServiceImpl implements EvaluationDetailService{
    private final SubmissionRepository submissionRepository;

    private final EvaluationRepository evaluationRepository;

    private final EvaluationDetailRepository evaluationDetailRepository;
    private final TeamRequestRepository teamRequestRepository;

    @Override
    public List<EvaluationResponse> getEvaluated(Integer submissionId) {
        Submission submission = submissionRepository.findById(submissionId).orElseThrow(() -> new BadRequestException("Không tìm thấy bài nộp này"));
        List<EvaluationResponse> listEvaluation = new ArrayList<>();

        List<Evaluation> evaluations = evaluationRepository.findBySubmission_SubmissionId(submissionId);

        if(evaluations.isEmpty()){
            throw new BadRequestException("Bài nộp chưa được chấm.");
        }

        for(Evaluation e: evaluations){
            List<EvaluationDetail> evaluationDetails = evaluationDetailRepository.findByEvaluation_EvaluationId(e.getEvaluationId());

            List<EvaluationDetailResponse> evaluationDetailResponses = evaluationDetails.stream().map(evaluationDetail -> this.mapToEvaluationDetailResponse(evaluationDetail)).toList();

            EvaluationResponse evaluationResponse = this.mapToEvaluationResponse(e, evaluationDetailResponses);

            listEvaluation.add(evaluationResponse);

        }
        return listEvaluation;
    }

    @Override
    public List<EvaluationResponse> getEvaluationDraftRankingAndAppeal(CustomUserDetails userDetails, Integer requestId) {
        Student student = userDetails.getAccount().getStudent();
        if(student == null ){
            throw new BadRequestException("Bạn không là Sinh viên.");
        }
        // Lấy điểm dc công bố ở draf final

        // BTC lấy các bài đã khiếu nại và dc chấm lại hoàn thành gửi về cho thí sinh (team)
//        TeamRequest teamRequest = teamRequestRepository.findById(requestId)
//                .orElseThrow(() -> new BadRequestException(" Không tìm thấy đơn khiếu nại"));
//        // Tìm các bài nộp đc chấm lại của tam yêu cầu khiếu nại
//        Evaluation evaluation = teamRequest.
//        // Thông qua ID request lấy các bài nộp đã được đánh giá lại
//
//
        return List.of();
    }

    private EvaluationDetailResponse mapToEvaluationDetailResponse(EvaluationDetail evaluationDetail){
        return EvaluationDetailResponse.builder().evaluationDetailId(evaluationDetail.getId())
                .criteriaName(evaluationDetail.getEvaluationCriteria().getCriteriaName())
                .score(evaluationDetail.getScore())
                .comment(evaluationDetail.getComment())
                .build();
    }

    private EvaluationResponse mapToEvaluationResponse(Evaluation evaluation, List<EvaluationDetailResponse> evaluationDetails){
        return EvaluationResponse.builder().evaluationId(evaluation.getEvaluationId())
                .listEvaluationDetail(evaluationDetails)
                .totalScore(evaluation.getScore())
                .status(evaluation.getStatus())
                .comment(evaluation.getComment())
                .build();
    }
}
