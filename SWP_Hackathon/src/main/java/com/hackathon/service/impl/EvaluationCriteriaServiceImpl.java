package com.hackathon.service.impl;

import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.entity.EvaluationCriteria;
import com.hackathon.repository.EvaluationCriteriaRepository;
import com.hackathon.service.EvaluationCriteriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.hackathon.entity.Round;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// Quản lý bản chụp tiêu chí chấm điểm được áp dụng cho từng vòng thi.
public class EvaluationCriteriaServiceImpl implements EvaluationCriteriaService {

    private final EvaluationCriteriaRepository evaluationCriteriaRepository;
    @Override
    // Tạo tiêu chí chấm điểm cho vòng dựa trên cấu hình được gửi lên.
    public EvaluationCriteria createEvaluationCritera(EvaluationCriteriaRequestDTO request, int criteriaSetId, Round round) {

        // Tạo bản ghi mới để giữ nguyên nội dung tiêu chí tại thời điểm cấu hình vòng.
        EvaluationCriteria evaluationCriteria = new EvaluationCriteria();
        // Gắn tiêu chí với vòng thi sẽ sử dụng tiêu chí này.
        evaluationCriteria.setRound(round);
        // Sao chép điểm tối đa từ bộ tiêu chí đang được vòng lựa chọn.
        evaluationCriteria.setMaxScore(round.getCriteriaSet().getMaxScore());
        // Lưu tên tiêu chí dùng khi giám khảo thực hiện chấm bài.
        evaluationCriteria.setCriteriaName(request.getCriteriaName());
        // Lưu trọng số riêng được cấu hình cho tiêu chí trong vòng.
        evaluationCriteria.setWeight(request.getCustomWeight());
        // Lưu mô tả để giám khảo hiểu yêu cầu cần đánh giá.
        evaluationCriteria.setDescription(request.getDescription());
        // Lưu loại tiêu chí để áp dụng đúng cách nhập hoặc tính điểm.
        evaluationCriteria.setType(request.getType());

        // Lưu bản chụp tiêu chí vào cơ sở dữ liệu.
        EvaluationCriteria saveEvaluationCriteria = evaluationCriteriaRepository.save(evaluationCriteria);

        // Trả về tiêu chí đã lưu để luồng tạo vòng tiếp tục sử dụng.
        return saveEvaluationCriteria;

    }

    @Override
    public EvaluationCriteriaResponseDTO mapToResponse(EvaluationCriteria evaluationCriteria) {
        return EvaluationCriteriaResponseDTO.builder()
                .evaluationCriteriaId(evaluationCriteria.getEvaluationCriteriaId())
                .customWeight(evaluationCriteria.getWeight())
                .criteriaName(evaluationCriteria.getCriteriaName())
                .type(evaluationCriteria.getType())
                .description(evaluationCriteria.getDescription())
                .build();
    }

    @Override
    // Lấy toàn bộ tiêu chí chấm điểm đang thuộc một vòng thi.
    public List<EvaluationCriteriaResponseDTO> getEvaluationCriteriaResponse(Round round) {

        // Không có vòng đầu vào thì không có tiêu chí nào để truy vấn.
        if(round == null){
            return new ArrayList<>();
        }
        // Tìm các tiêu chí theo mã vòng thi.
        List<EvaluationCriteria> evaluationCriterias = evaluationCriteriaRepository.findByRound_RoundId(round.getRoundId());
        // Trả danh sách rỗng khi vòng chưa được cấu hình tiêu chí.
        if(evaluationCriterias == null || evaluationCriterias.isEmpty()){
            return new ArrayList<>();
        }

        return evaluationCriterias.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    // Xóa toàn bộ tiêu chí chấm điểm gắn với một vòng thi.
    public void deletedEvaluationCriteria(Integer roundId) {
        // Thực hiện xóa theo mã vòng để làm sạch cấu hình cũ trước khi cập nhật.
        evaluationCriteriaRepository.deleteByRound_RoundId(roundId);
    }


}
