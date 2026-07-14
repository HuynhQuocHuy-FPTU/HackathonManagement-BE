package com.hackathon.service.grading;

import com.hackathon.entity.*;
import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.CategoryRoundRepository;
import com.hackathon.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScoreExportService {
    private final EvaluationRepository evaluationRepository;
    private final CategoryRoundRepository categoryRoundRepository;

    private static final String[] HEADER = {
            "ItemID", "RaterID", "CriteriaID", "CriteriaName", "Score", "Weight"
    };

    /**
     * Xuất CSV ẩn danh cho toàn bộ điểm GRADED trong 1 categoryRound.
     */
    public byte[] exportAnonymizedScores(Integer categoryRoundId) {
        categoryRoundRepository.findById(categoryRoundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy category round"));

        List<Evaluation> evaluations = evaluationRepository.findBySubmission_TeamParticipant_CategoryRound_CategoryRoundIdAndStatus(categoryRoundId, EvaluationStatus.GRADED);

        // Map ẩn danh — chỉ tồn tại trong phạm vi 1 lần gọi hàm này, không lưu lại ở đâu.
        Map<Integer, String> itemAnonymizedIds = new LinkedHashMap<>(); // key: teamParticipant.id
        Map<Integer, String> raterAnonymizedIds = new LinkedHashMap<>(); // key: expert.id

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            // UTF-8 BOM để Excel mở tiếng Việt không bị lỗi font
            writer.write('\uFEFF');
            writeRow(writer, HEADER);

            for (Evaluation evaluation : evaluations) {
                TeamParticipant participant = evaluation.getSubmission().getTeamParticipant();
                Expert expert = evaluation.getExpertAssign().getExpert();

                String itemId = itemAnonymizedIds.computeIfAbsent(
                        participant.getId(), k -> "Item-" + (itemAnonymizedIds.size() + 1));
                String raterId = raterAnonymizedIds.computeIfAbsent(
                        expert.getExpertId(), k -> "Rater-" + (raterAnonymizedIds.size() + 1));

                for (EvaluationDetail detail : evaluation.getEvaluationDetails()) {
                    EvaluationCriteria criteria = detail.getEvaluationCriteria();

                    writeRow(writer,
                            itemId,
                            raterId,
                            criteria != null ? String.valueOf(criteria.getEvaluationCriteriaId()) : "",
                            criteria != null ? criteria.getCriteriaName() : "",
                            detail.getScore() != null ? detail.getScore().toPlainString() : "",
                            criteria != null && criteria.getWeight() != null ? criteria.getWeight().toPlainString() : ""
                    );
                }
            }
        }

        return out.toByteArray();
    }

    private void writeRow(PrintWriter writer, String... columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escapeCsv(columns[i]));
        }
        writer.println(sb);
    }

    /**
     * Escape đúng chuẩn CSV: nếu giá trị chứa dấu phẩy, dấu ngoặc kép, hoặc xuống dòng
     * thì bọc trong ngoặc kép và nhân đôi ngoặc kép bên trong (theo RFC 4180).
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }
}
