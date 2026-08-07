package com.hackathon.service.impl;

import com.hackathon.dto.evaluation.EvaluationDetailResponse;
import com.hackathon.dto.evaluation.EvaluationResponse;
import com.hackathon.entity.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.service.EvaluationDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
// Truy xuất kết quả chấm tổng và điểm chi tiết theo từng tiêu chí của bài nộp.
public class EvaluationDetailServiceImpl implements EvaluationDetailService {
    private final SubmissionRepository submissionRepository;

    private final EvaluationRepository evaluationRepository;

    private final EvaluationDetailRepository evaluationDetailRepository;


    @Override
    // Lấy toàn bộ lượt đánh giá đã được tạo cho một bài nộp.
    public List<EvaluationResponse> getEvaluated(Integer submissionId) {
        // Xác nhận bài nộp tồn tại trước khi truy vấn kết quả đánh giá liên quan.
        Submission submission = submissionRepository.findById(submissionId).orElseThrow(() -> new BadRequestException("Không tìm thấy bài nộp này"));
        // Chuẩn bị danh sách chứa kết quả của từng giám khảo.
        List<EvaluationResponse> listEvaluation = new ArrayList<>();

        // Lấy tất cả lượt đánh giá được gắn với bài nộp.
        List<Evaluation> evaluations = evaluationRepository.findBySubmission_SubmissionId(submissionId);

        // Báo rõ bài chưa được chấm nếu chưa có bất kỳ lượt đánh giá nào.
        if(evaluations.isEmpty()){
            throw new BadRequestException("Bài nộp chưa được chấm.");
        }

        // Xử lý từng lượt đánh giá để lấy đầy đủ điểm thành phần.
        for(Evaluation e: evaluations){
            // Tìm điểm và nhận xét của từng tiêu chí thuộc lượt đánh giá hiện tại.
            List<EvaluationDetail> evaluationDetails = evaluationDetailRepository.findByEvaluation_EvaluationId(e.getEvaluationId());

            List<EvaluationDetailResponse> evaluationDetailResponses = evaluationDetails.stream().map(evaluationDetail -> this.mapToEvaluationDetailResponse(evaluationDetail)).toList();

            EvaluationResponse evaluationResponse = this.mapToEvaluationResponse(e, evaluationDetailResponses);

            // Thêm kết quả của giám khảo hiện tại vào danh sách chung của bài nộp.
            listEvaluation.add(evaluationResponse);

        }
        // Trả về toàn bộ kết quả chấm đã thu thập.
        return listEvaluation;
    }


    private EvaluationDetailResponse mapToEvaluationDetailResponse(EvaluationDetail evaluationDetail){
        return EvaluationDetailResponse.builder().evaluationDetailId(evaluationDetail.getId())
                .criteriaName(evaluationDetail.getEvaluationCriteria().getCriteriaName())
                .criteriaDescription(evaluationDetail.getEvaluationCriteria().getDescription())
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
