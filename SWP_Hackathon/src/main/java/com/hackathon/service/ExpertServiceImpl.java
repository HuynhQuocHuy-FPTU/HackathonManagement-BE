package com.hackathon.service;

import com.hackathon.dto.expert.ExpertInfoResponse;
import com.hackathon.dto.expert.ExpertOverviewResponse;
import com.hackathon.dto.team.TeamDetailResponse;
import com.hackathon.entity.Evaluation;
import com.hackathon.entity.Expert;
import com.hackathon.entity.ExpertAssign;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.EvaluationRepository;
import com.hackathon.repository.ExpertRepository;
import com.hackathon.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpertServiceImpl implements ExpertService {

    @Autowired
    private ExpertRepository expertRepository;
    @Autowired
    private EvaluationRepository evaluationRepository;

    @Override
    public List<ExpertInfoResponse> getAllExperts() {
        List<Expert> experts = expertRepository.findAll();

        //chuyển expert thành response
        return experts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public ExpertInfoResponse getExpertById(Integer id) {
        Expert expert = expertRepository.findById(id).orElseThrow(() -> new BadRequestException("Không tìm thấy expert"));

        return this.mapToResponse(expert);
    }

    @Override
    public ExpertInfoResponse mapToResponse(Expert expert) {
        if (expert == null) return null;
        return ExpertInfoResponse.builder()
                .expertId(expert.getExpertId())
                .expertName(expert.getExpertName())
                .build();
    }

    @Override
    public ExpertOverviewResponse getExpertOverview(CustomUserDetails userDetails, Integer eventId) {
        Expert expert = expertRepository.findByAccount_AccountId(userDetails.getAccount().getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Expert."));
        long totalAssigned = evaluationRepository.countTotalAssigned(expert.getExpertId(), eventId);
        // Số bài đã hoàn thành ở thời điểm hiện tại.
        long completedReviews = evaluationRepository.countCompletedReviews(expert.getExpertId(), eventId);
        //   Số bài đang cần expert xử lý.
        long pendingReviews = evaluationRepository.countPendingReviews(expert.getExpertId(), eventId);
        //Số bài đang chờ chấm lại.
        long reEvaluationReviews = evaluationRepository.reEvaluationReviews(expert.getExpertId(), eventId);
        return new ExpertOverviewResponse(
                totalAssigned,
                completedReviews,
                pendingReviews,
                reEvaluationReviews
        );
    }


}
