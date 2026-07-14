package com.hackathon.validator;

import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Round;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.CategoryRoundRepository;
import com.hackathon.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdvancementValidator {

    private final RoundRepository roundRepository;
    private final CategoryRoundRepository categoryRoundRepository;

    public void validateCategoryRoundAdvancement(CategoryRound currentCategoryRound) {
        Round currentRound = currentCategoryRound.getRound();

        // 1. Kiểm tra thời gian đánh giá
        validateEvaluationPeriod(currentRound);

        // 2. Kiểm tra cấu hình số lượng đội thăng vòng tối đa (Top N)
        validateTopNConfig(currentRound);

        // 3. Kiểm tra xem vòng hiện tại có phải vòng cuối cùng (Chung kết) không
        validateHasNextRound(currentRound);

        // 4. Kiểm tra xem Chuyên mục (Category) này đã được cấu hình ở vòng tiếp theo chưa
        validateNextCategoryRoundConfigured(currentCategoryRound);

    }

    private void validateEvaluationPeriod(Round currentRound) {
        RoundStatus currentStatus = currentRound.getStatus();
        if (currentStatus == RoundStatus.EVALUATING ) {
            throw new BadRequestException("Chưa kết thúc thời gian chấm điểm cho vòng thi");
        }
    }

    private void validateTopNConfig(Round currentRound) {
        if (currentRound.getTopN() == null || currentRound.getTopN() <= 0) {
            throw new BadRequestException(
                    "Round '" + currentRound.getRoundName() + "' chưa được cấu hình Top_N hợp lệ. Vui lòng cập nhật trước khi thăng vòng.");
        }
    }

    private void validateHasNextRound(Round currentRound) {
        boolean hasNextRound = roundRepository.findRoundByHackathonEvent_EventIdAndOrderIndex(
                currentRound.getHackathonEvent().getEventId(),
                currentRound.getOrderIndex() + 1
        ).isPresent();

        if (!hasNextRound) {
            throw new BadRequestException("Vòng '" + currentRound.getRoundName() + "' đã là vòng cuối cùng của sự kiện, không thể thực hiện thăng vòng.");
        }
    }

    private void validateNextCategoryRoundConfigured(CategoryRound currentCategoryRound) {
        Round currentRound = currentCategoryRound.getRound();

        Round nextRound = roundRepository.findRoundByHackathonEvent_EventIdAndOrderIndex(
                currentRound.getHackathonEvent().getEventId(),
                currentRound.getOrderIndex() + 1
        ).get();

        boolean hasNextCategoryRound = categoryRoundRepository.findCategoryRoundByCategory_CategoryIdAndRound_RoundId(
                currentCategoryRound.getCategory().getCategoryId(),
                nextRound.getRoundId()
        ).isPresent();

        if (!hasNextCategoryRound) {
            throw new ResourceNotFoundException(
                    "Lỗi cấu hình hệ thống: Chuyên mục '" + currentCategoryRound.getCategory().getCategoryName()
                            + "' chưa được thiết lập cho vòng tiếp theo (Thiếu bản ghi CategoryRound).");
        }
    }

}
