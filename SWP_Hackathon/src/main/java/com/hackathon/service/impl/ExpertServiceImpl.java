package com.hackathon.service.impl;

import com.hackathon.dto.expert.ExpertInfoResponse;
import com.hackathon.dto.expert.ExpertOverviewResponse;
import com.hackathon.entity.Expert;
import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.entity.enums.ExpertRole;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.EvaluationRepository;
import com.hackathon.repository.ExpertRepository;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.ExpertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
// Cung cấp thông tin chuyên gia và thống kê tiến độ chấm bài của chuyên gia.
public class ExpertServiceImpl implements ExpertService {

    @Autowired
    private ExpertRepository expertRepository;
    @Autowired
    private EvaluationRepository evaluationRepository;
    @Override
    // Lấy toàn bộ chuyên gia hiện có trong hệ thống.
    public List<ExpertInfoResponse> getAllExperts() {
        // Truy vấn danh sách chuyên gia từ cơ sở dữ liệu.
        List<Expert> experts = expertRepository.findAll();

        return experts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    // Tìm thông tin một chuyên gia theo mã chuyên gia.
    public ExpertInfoResponse getExpertById(Integer id) {
        // Báo lỗi khi mã được yêu cầu không thuộc chuyên gia nào.
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
    // Tổng hợp số bài đã giao, đã chấm, đang chờ và đang chấm lại của chuyên gia trong sự kiện.
    public ExpertOverviewResponse getExpertOverview(CustomUserDetails userDetails, Integer eventId) {
        // Tìm hồ sơ chuyên gia theo tài khoản đang đăng nhập.
        Expert expert = expertRepository.findByAccount_AccountId(userDetails.getAccount().getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Expert."));

        // Chỉ tính các phân công có vai trò giám khảo chính hoặc giám khảo khách mời.
        List<ExpertRole> judgeRoles = List.of(ExpertRole.CORE_JUDGE, ExpertRole.GUEST_JUDGE);

        // Đếm tổng số bài được giao cho chuyên gia trong sự kiện.
        long totalAssigned = evaluationRepository.countTotalAssigned(expert.getExpertId(), eventId, judgeRoles);

        // Đếm các bài đã chấm xong, bao gồm cả bài hoàn tất sau khi chấm lại.
        long completedReviews = evaluationRepository.countReviewsByStatuses(
                expert.getExpertId(),
                eventId,
                List.of(EvaluationStatus.GRADED, EvaluationStatus.RE_EVALUATED),
                judgeRoles
        );

        // Đếm các bài đang chờ chuyên gia thực hiện chấm lại do khiếu nại.
        long reEvaluationReviews = evaluationRepository.countReviewsByStatuses(
                expert.getExpertId(),
                eventId,
                List.of(EvaluationStatus.RE_EVALUATION),
                judgeRoles
        );

        // Bài chờ chấm bằng tổng số bài được giao trừ bài đã hoàn tất và bài đang chấm lại.
        long pendingReviews = totalAssigned - completedReviews - reEvaluationReviews;
        // Giữ số bài chờ ở mức không âm nếu dữ liệu trạng thái có chênh lệch tạm thời.
        if (pendingReviews < 0) pendingReviews = 0;

        return new ExpertOverviewResponse(
                totalAssigned,
                completedReviews,
                pendingReviews,
                reEvaluationReviews
        );
    }


}
