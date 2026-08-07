package com.hackathon.service.grading.support;

import com.hackathon.dto.evaluation.CriteriaScoreRequest;
import com.hackathon.dto.evaluation.SubmitEvaluationRequest;
import com.hackathon.entity.EvaluationCriteria;
import com.hackathon.entity.enums.CriteriaType;
import com.hackathon.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Trách nhiệm: Kiểm tra tính toàn vẹn dữ liệu điểm số dựa trên cấu hình tiêu chí (Criteria Domain).
 */
@Component
public class CriteriaCompletenessValidator {

    public void validate(SubmitEvaluationRequest request, List<EvaluationCriteria> roundCriteria) {
        if (roundCriteria == null || roundCriteria.isEmpty()) {
            throw new BadRequestException("Vòng thi này chưa được cấu hình tiêu chí đánh giá.");
        }

        // Tối ưu: Lấy maxScore duy nhất từ phần tử đầu tiên (vì tất cả tiêu chí cùng round_id có chung maxScore)
        BigDecimal maxScale = BigDecimal.valueOf(roundCriteria.get(0).getMaxScore());

        // KIỂM TRA ĐIỂM VƯỢT KHUNG DỰA TRÊN CẤU HÌNH MAX SCORE
        for (CriteriaScoreRequest scoreReq : request.getCriteriaScores()) {
            if (scoreReq.getScore().compareTo(maxScale) > 0) {
                throw new BadRequestException("Điểm số " + scoreReq.getScore() +
                        " không hợp lệ! Vòng thi này sử dụng thang điểm tối đa là: " + maxScale);
            }
        }

        Map<Integer, EvaluationCriteria> criteriaMap = roundCriteria.stream()
                .collect(Collectors.toMap(EvaluationCriteria::getEvaluationCriteriaId, c -> c));

        // Trích xuất danh sách ID tiêu chí mà Frontend gửi lên
        Set<Integer> requestCriteriaIds = request.getCriteriaScores().stream()
                .map(CriteriaScoreRequest::getEvaluationCriteriaId)
                .collect(Collectors.toSet());

        // Kiểm tra tính hợp lệ: ID tiêu chí gửi lên phải nằm trong cấu hình của Vòng thi hiện hành
        requestCriteriaIds.forEach(id -> {
            if (!criteriaMap.containsKey(id)) {
                throw new BadRequestException("Mã tiêu chí đánh giá (ID: " + id + ") không tồn tại hoặc không thuộc vòng thi này.");
            }
        });

        // Xác định các nhóm Loại tiêu chí
        Set<CriteriaType> affectedTypes = requestCriteriaIds.stream()
                .map(id -> criteriaMap.get(id).getType())
                .collect(Collectors.toSet());

        // Gom nhóm toàn bộ tiêu chí
        Map<CriteriaType, List<EvaluationCriteria>> systemCriteriaGroupedByType = roundCriteria.stream()
                .collect(Collectors.groupingBy(EvaluationCriteria::getType));

        // Đối chiếu tính toàn vẹn
        for (CriteriaType type : affectedTypes) {
            Set<Integer> expectedIdsOfThisType = systemCriteriaGroupedByType.getOrDefault(type, List.of()).stream()
                    .map(EvaluationCriteria::getEvaluationCriteriaId)
                    .collect(Collectors.toSet());

            if (!requestCriteriaIds.containsAll(expectedIdsOfThisType)) {
                throw new BadRequestException("Tính toàn vẹn dữ liệu thất bại: Bạn bắt buộc phải nhập đầy đủ điểm số cho toàn bộ "
                        + expectedIdsOfThisType.size() + " tiêu chí thuộc nhóm '" + type + "' trong một lần thực thi.");
            }
        }
    }

    public void validatePartial(SubmitEvaluationRequest request, List<EvaluationCriteria> roundCriteria, CriteriaType targetType) {
        if (roundCriteria == null || roundCriteria.isEmpty()) {
            throw new BadRequestException("Vòng thi này chưa được cấu hình tiêu chí đánh giá.");
        }

        // Tối ưu: Lấy maxScale duy nhất từ phần tử đầu tiên
        BigDecimal maxScale = BigDecimal.valueOf(roundCriteria.getFirst().getMaxScore());

        for (CriteriaScoreRequest scoreReq : request.getCriteriaScores()) {
            if (scoreReq.getScore().compareTo(maxScale) > 0) {
                throw new BadRequestException("Điểm số " + scoreReq.getScore() + " vượt quá thang điểm " + maxScale);
            }
        }

        List<EvaluationCriteria> targetCriteriaList = roundCriteria.stream()
                .filter(c -> c.getType() == targetType)
                .collect(Collectors.toList());

        if (targetCriteriaList.isEmpty()) {
            throw new BadRequestException("Vòng thi này không cấu hình tiêu chí chấm điểm cho phần: " + targetType);
        }

        Map<Integer, EvaluationCriteria> targetCriteriaMap = targetCriteriaList.stream()
                .collect(Collectors.toMap(EvaluationCriteria::getEvaluationCriteriaId, c -> c));

        Set<Integer> requestIds = request.getCriteriaScores().stream()
                .map(CriteriaScoreRequest::getEvaluationCriteriaId)
                .collect(Collectors.toSet());

        for (Integer reqId : requestIds) {
            if (!targetCriteriaMap.containsKey(reqId)) {
                throw new BadRequestException("Tiêu chí ID " + reqId + " không hợp lệ hoặc không thuộc phần " + targetType);
            }
        }

        if (!requestIds.containsAll(targetCriteriaMap.keySet())) {
            throw new BadRequestException("Bạn phải chấm ĐẦY ĐỦ tất cả các tiêu chí của phần " + targetType);
        }
    }
}
