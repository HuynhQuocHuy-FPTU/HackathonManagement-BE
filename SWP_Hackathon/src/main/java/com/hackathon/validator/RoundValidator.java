package com.hackathon.validator;

import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.dto.round.UpdateRoundRequest;
import com.hackathon.dto.round.UpdateTimeRoundRequest;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.Round;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.exception.BadRequestException;
import lombok.Data;
import org.springframework.stereotype.Component;

import javax.swing.plaf.PanelUI;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Data
@Component
public class RoundValidator {

    public void validatorCreate(CreateRoundRequest request, HackathonEvent event) throws BadRequestException {
        validatePartialDeadlineOrder(
                request.getStartDate(),
                request.getSubmissionDeadline(),
                request.getEvaluationDeadline(),
                request.getResolveAppealDeadline(),
                request.getEndDate()
        );

        // Kiểm tra ngày bắt đầu và kết thúc của round có sau ngày bắt đầu của event và trước ngày kết thúc của event
        if ((request.getStartDate() != null
                && event.getStartDate() != null
                && request.getStartDate().isBefore(event.getStartDate()))
                || (request.getEndDate() != null
                && event.getEndDate() != null
                && request.getEndDate().isAfter(event.getEndDate()))) {
            throw new BadRequestException("Thời gian bắt đầu và kết thúc của round phải nằm trong thời gian của event");
        }
    }

    public void validatorUpdate(UpdateRoundRequest request) throws BadRequestException {
        validatePartialDeadlineOrder(
                request.getStartDate(),
                request.getSubmissionDeadline(),
                request.getEvaluationDeadline(),
                request.getResolveAppealDeadline(),
                request.getEndDate()
        );
    }

    public void validateTimelineByOrderIndex(CreateRoundRequest request, List<Round> currentRounds) throws BadRequestException {
        if (currentRounds == null || currentRounds.isEmpty()
                || request.getOrderIndex() == null) {
            return;
        }

        for (Round existingRound : currentRounds) {
            if (existingRound.getOrderIndex() == null) {
                continue;
            }
            if (request.getOrderIndex().equals(existingRound.getOrderIndex())) {
                throw new BadRequestException("Thứ tự vòng thi (orderIndex = " + request.getOrderIndex() + ") đã tồn tại trong sự kiện này!");
            }

            if (request.getOrderIndex() > existingRound.getOrderIndex()) {
                if (request.getStartDate() != null
                        && existingRound.getEndTime() != null
                        && request.getStartDate().isBefore(existingRound.getEndTime())) {
                    throw new BadRequestException(String.format(
                            "Vòng thi '%s' (thứ tự %d) phải bắt đầu sau khi vòng '%s' (thứ tự %d) kết thúc (sau ngày %s)!",
                            request.getRoundName(),
                            request.getOrderIndex(), existingRound.getRoundName(), existingRound.getOrderIndex(), existingRound.getEndTime()
                    ));
                }
            }

            if (request.getOrderIndex() < existingRound.getOrderIndex()) {
                if (request.getEndDate() != null
                        && existingRound.getStartTime() != null
                        && request.getEndDate().isAfter(existingRound.getStartTime())) {
                    throw new BadRequestException(String.format(
                            "Vòng thi mới (thứ tự %d) phải kết thúc trước khi vòng '%s' (thứ tự %d) bắt đầu (trước ngày %s)!",
                            request.getOrderIndex(), existingRound.getRoundName(), existingRound.getOrderIndex(), existingRound.getStartTime()
                    ));
                }
            }
        }
    }

    public void validateTimelineByOrderIndexUpdate(UpdateRoundRequest request, List<Round> currentRounds) throws BadRequestException {
        if (currentRounds == null || currentRounds.isEmpty()
                || request.getOrderIndex() == null) {
            return;
        }

        for (Round existingRound : currentRounds) {
            if (request.getRoundId() != null && request.getRoundId().equals(existingRound.getRoundId())) {
                continue;
            }

            if (existingRound.getOrderIndex() == null) {
                continue;
            }
            if (request.getOrderIndex().equals(existingRound.getOrderIndex())) {
                throw new BadRequestException("Thứ tự vòng thi (orderIndex = " + request.getOrderIndex() + ") đã tồn tại trong sự kiện này!");
            }

            if (request.getOrderIndex() > existingRound.getOrderIndex()) {
                if (request.getStartDate() != null
                        && existingRound.getEndTime() != null
                        && request.getStartDate().isBefore(existingRound.getEndTime())) {
                    throw new BadRequestException(String.format(
                            "Vòng thi chỉnh sửa (thứ tự %d) phải bắt đầu sau khi vòng '%s' (thứ tự %d) kết thúc (sau ngày %s)!",
                            request.getOrderIndex(), existingRound.getRoundName(), existingRound.getOrderIndex(), existingRound.getEndTime()
                    ));
                }
            }

            if (request.getOrderIndex() < existingRound.getOrderIndex()) {
                if (request.getEndDate() != null
                        && existingRound.getStartTime() != null
                        && request.getEndDate().isAfter(existingRound.getStartTime())) {
                    throw new BadRequestException(String.format(
                            "Vòng thi chỉnh sửa (thứ tự %d) phải kết thúc trước khi vòng '%s' (thứ tự %d) bắt đầu (trước ngày %s)!",
                            request.getOrderIndex(), existingRound.getRoundName(), existingRound.getOrderIndex(), existingRound.getStartTime()
                    ));
                }
            }
        }
    }


     //Kiểm tra timeline của toàn bộ danh sách Round Request từ client gửi lên (RAM Check)
    public void validateRoundsTimelineByOrder(List<UpdateRoundRequest> roundRequests) throws BadRequestException {
        if (roundRequests == null || roundRequests.size() <= 1) {
            return; // 0 hoặc 1 round thì không có gì để đá nhau
        }

        // 1. Sắp xếp danh sách request theo thứ tự orderIndex tăng dần
        List<UpdateRoundRequest> sortedRequests = roundRequests.stream()
                .filter(request -> request.getOrderIndex() != null)
                .sorted(Comparator.comparing(UpdateRoundRequest::getOrderIndex))
                .toList();

        // 2. Chạy vòng lặp so sánh cặp kế tiếp (vòng sau so với vòng trước liền kề)
        for (int i = 0; i < sortedRequests.size() - 1; i++) {
            UpdateRoundRequest current = sortedRequests.get(i);
            UpdateRoundRequest next = sortedRequests.get(i + 1);

            // Kiểm tra trùng orderIndex ngay trên request gửi lên
            if (current.getOrderIndex().equals(next.getOrderIndex())) {
                throw new BadRequestException("Có hai vòng thi bị trùng thứ tự hiển thị (orderIndex = " + current.getOrderIndex() + ")!");
            }

            // Vòng đứng sau (next) phải bắt đầu sau khi vòng đứng trước (current) kết thúc hoàn toàn
            if (next.getStartDate() != null
                    && current.getEndDate() != null
                    && next.getStartDate().isBefore(current.getEndDate())) {
                throw new BadRequestException(String.format(
                        "Lỗi logic dòng thời gian: Vòng '%s' (thứ tự %d) phải bắt đầu sau khi vòng '%s' (thứ tự %d) kết thúc (sau ngày %s)!",
                        next.getRoundName(), next.getOrderIndex(), current.getRoundName(), current.getOrderIndex(), current.getEndDate()
                ));
            }
        }
    }

    public void validateTimeRound(Round round, UpdateTimeRoundRequest request){
        if (round == null || request == null) {
            throw new BadRequestException(
                    "Thông tin cập nhật thời gian vòng thi không hợp lệ"
            );
        }
        if (round.getStatus() == RoundStatus.COMPLETED) {
            throw new BadRequestException(
                    "Không thể cập nhật thời gian của vòng thi đã hoàn thành"
            );
        }

        LocalDateTime startTime = request.getStartDate();
        LocalDateTime submissionDeadline = request.getSubmissionDeadline();
        LocalDateTime evaluationDeadline = request.getEvaluationDeadline();
        LocalDateTime resolveAppealDeadline =
                request.getResolveAppealDeadline();
        LocalDateTime endTime = request.getEndDate();

        validateCompleteDeadlineOrder(
                startTime,
                submissionDeadline,
                evaluationDeadline,
                resolveAppealDeadline,
                endTime
        );

        HackathonEvent event = round.getHackathonEvent();

        if (event == null) {
            throw new BadRequestException("Vòng thi chưa thuộc sự kiện nào");
        }

        if (startTime.isBefore(event.getStartDate())
                || endTime.isAfter(event.getEndDate())) {
            throw new BadRequestException("Thời gian vòng thi phải nằm trong thời gian của sự kiện"
            );
        }

        if (event.getRounds() == null) {
            return;
        }

        // Kiểm tra timeline với các round khác.
        for (Round otherRound : event.getRounds()) {
            if (otherRound.getRoundId().equals(round.getRoundId())) {
                continue;
            }

            // Round hiện tại phải bắt đầu sau các round đứng trước.
            if (otherRound.getOrderIndex() < round.getOrderIndex()
                    && startTime.isBefore(otherRound.getEndTime())) {
                throw new BadRequestException(String.format(
                        "Vòng thứ %d phải bắt đầu sau khi vòng '%s' kết thúc lúc %s",
                        round.getOrderIndex(),
                        otherRound.getRoundName(),
                        otherRound.getEndTime()
                ));
            }

            // Round hiện tại phải kết thúc trước các round đứng sau.
            if (otherRound.getOrderIndex() > round.getOrderIndex()
                    && endTime.isAfter(otherRound.getStartTime())) {
                throw new BadRequestException(String.format(
                        "Vòng thứ %d phải kết thúc trước khi vòng '%s' bắt đầu lúc %s",
                        round.getOrderIndex(),
                        otherRound.getRoundName(),
                        otherRound.getStartTime()
                ));
            }
        }


    }

    public void validateRoundForPublish(
            Round round,
            HackathonEvent event
    ) {
        validateCompleteDeadlineOrder(
                round.getStartTime(),
                round.getSubmissionDeadline(),
                round.getEvaluationDeadline(),
                round.getResolveAppealDeadline(),
                round.getEndTime()
        );

        if (round.getStartTime().isBefore(event.getStartDate())
                || round.getEndTime().isAfter(event.getEndDate())) {
            throw new BadRequestException(
                    "Thời gian vòng thi phải nằm trong thời gian của sự kiện"
            );
        }
    }

    private void validateCompleteDeadlineOrder(
            LocalDateTime startTime,
            LocalDateTime submissionDeadline,
            LocalDateTime evaluationDeadline,
            LocalDateTime resolveAppealDeadline,
            LocalDateTime endTime
    ) {
        if (startTime == null || submissionDeadline == null
                || evaluationDeadline == null || resolveAppealDeadline == null
                || endTime == null) {
            throw new BadRequestException("Các mốc thời gian của vòng thi không được để trống");
        }

        if (!startTime.isBefore(submissionDeadline)) {
            throw new BadRequestException("Hạn nộp bài phải sau thời gian bắt đầu vòng");
        }

        if (!submissionDeadline.isBefore(evaluationDeadline)) {
            throw new BadRequestException("Thời gian kết thúc đánh giá phải sau hạn nộp bài");
        }

        if (!evaluationDeadline.isBefore(resolveAppealDeadline)) {
            throw new BadRequestException("Thời gian xử lý khiếu nại phải sau thời gian kết thúc đánh giá");
        }

        if (resolveAppealDeadline.isAfter(endTime)) {
            throw new BadRequestException("Thời gian xử lý khiếu nại không được sau thời gian kết thúc vòng");
        }
    }

    private void validatePartialDeadlineOrder(
            LocalDateTime startTime,
            LocalDateTime submissionDeadline,
            LocalDateTime evaluationDeadline,
            LocalDateTime resolveAppealDeadline,
            LocalDateTime endTime
    ) {
        if (startTime != null && submissionDeadline != null
                && !startTime.isBefore(submissionDeadline)) {
            throw new BadRequestException(
                    "Hạn nộp bài phải sau thời gian bắt đầu vòng"
            );
        }

        if (submissionDeadline != null && evaluationDeadline != null
                && !submissionDeadline.isBefore(evaluationDeadline)) {
            throw new BadRequestException(
                    "Thời gian kết thúc đánh giá phải sau hạn nộp bài"
            );
        }

        if (evaluationDeadline != null && resolveAppealDeadline != null
                && !evaluationDeadline.isBefore(resolveAppealDeadline)) {
            throw new BadRequestException(
                    "Thời gian xử lý khiếu nại phải sau thời gian kết thúc đánh giá"
            );
        }

        if (resolveAppealDeadline != null && endTime != null
                && resolveAppealDeadline.isAfter(endTime)) {
            throw new BadRequestException(
                    "Thời gian xử lý khiếu nại không được sau thời gian kết thúc vòng"
            );
        }
    }
}
