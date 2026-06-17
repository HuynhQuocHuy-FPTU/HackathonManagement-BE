package com.hackathon.validator;

import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.dto.round.UpdateRoundRequest;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.Round;
import com.hackathon.exception.BadRequestException;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Data
@Component
public class RoundValidator {

    public void validatorCreate(CreateRoundRequest request, HackathonEvent event) throws BadRequestException {
        // Check time cơ bản
        if(request.getStartDate().isAfter(request.getEndDate())){
            throw new BadRequestException("Start date must be before end date");
        }

        // Kiểm tra hạn nộp bài cho mỗi vòng
        if(request.getSubmissionDeadline().isBefore(request.getStartDate()) || request.getSubmissionDeadline().isAfter(request.getEndDate())){
            throw new BadRequestException("Submission deadline phải sau startDate và trước endDate");
        }

        // Kiểm tra ngày bắt đầu và kết thúc của round có sau ngày bắt đầu của event và trước ngày kết thúc của event
        if(request.getStartDate().isBefore(event.getStartDate()) || request.getEndDate().isAfter(event.getEndDate())){
            throw new BadRequestException("Thời gian bắt đầu và kết thúc của round phải nằm trong thời gian của event");
        }
    }

    public void validatorUpdate(UpdateRoundRequest request) throws BadRequestException {
        // Check time cơ bản
        if(request.getStartDate().isAfter(request.getEndDate())){
            throw new BadRequestException("Start date must be before end date");
        }

        // 💡 BỔ SUNG: Kiểm tra hạn nộp bài khi update
        if(request.getSubmissionDeadline() != null) {
            if(request.getSubmissionDeadline().isBefore(request.getStartDate()) || request.getSubmissionDeadline().isAfter(request.getEndDate())){
                throw new BadRequestException("Submission deadline phải sau startDate và trước endDate");
            }
        }
    }

    public void validateTimelineByOrderIndex(CreateRoundRequest request, List<Round> currentRounds) throws BadRequestException {
        if (currentRounds == null || currentRounds.isEmpty()) {
            return;
        }

        for (Round existingRound : currentRounds) {
            if (request.getOrderIndex().equals(existingRound.getOrderIndex())) {
                throw new BadRequestException("Thứ tự vòng thi (orderIndex = " + request.getOrderIndex() + ") đã tồn tại trong sự kiện này!");
            }

            if (request.getOrderIndex() > existingRound.getOrderIndex()) {
                if (request.getStartDate().isBefore(existingRound.getEndTime())) {
                    throw new BadRequestException(String.format(
                            "Vòng thi '%s' (thứ tự %d) phải bắt đầu sau khi vòng '%s' (thứ tự %d) kết thúc (sau ngày %s)!",
                            request.getRoundName(),
                            request.getOrderIndex(), existingRound.getRoundName(), existingRound.getOrderIndex(), existingRound.getEndTime()
                    ));
                }
            }

            if (request.getOrderIndex() < existingRound.getOrderIndex()) {
                if (request.getEndDate().isAfter(existingRound.getStartTime())) {
                    throw new BadRequestException(String.format(
                            "Vòng thi mới (thứ tự %d) phải kết thúc trước khi vòng '%s' (thứ tự %d) bắt đầu (trước ngày %s)!",
                            request.getOrderIndex(), existingRound.getRoundName(), existingRound.getOrderIndex(), existingRound.getStartTime()
                    ));
                }
            }
        }
    }

    public void validateTimelineByOrderIndexUpdate(UpdateRoundRequest request, List<Round> currentRounds) throws BadRequestException {
        if (currentRounds == null || currentRounds.isEmpty()) {
            return;
        }

        for (Round existingRound : currentRounds) {
            if (request.getRoundId() != null && request.getRoundId().equals(existingRound.getRoundId())) {
                continue;
            }

            if (request.getOrderIndex().equals(existingRound.getOrderIndex())) {
                throw new BadRequestException("Thứ tự vòng thi (orderIndex = " + request.getOrderIndex() + ") đã tồn tại trong sự kiện này!");
            }

            if (request.getOrderIndex() > existingRound.getOrderIndex()) {
                if (request.getStartDate().isBefore(existingRound.getEndTime())) {
                    throw new BadRequestException(String.format(
                            "Vòng thi chỉnh sửa (thứ tự %d) phải bắt đầu sau khi vòng '%s' (thứ tự %d) kết thúc (sau ngày %s)!",
                            request.getOrderIndex(), existingRound.getRoundName(), existingRound.getOrderIndex(), existingRound.getEndTime()
                    ));
                }
            }

            if (request.getOrderIndex() < existingRound.getOrderIndex()) {
                if (request.getEndDate().isAfter(existingRound.getStartTime())) {
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
            if (next.getStartDate().isBefore(current.getEndDate())) {
                throw new BadRequestException(String.format(
                        "Lỗi logic dòng thời gian: Vòng '%s' (thứ tự %d) phải bắt đầu sau khi vòng '%s' (thứ tự %d) kết thúc (sau ngày %s)!",
                        next.getRoundName(), next.getOrderIndex(), current.getRoundName(), current.getOrderIndex(), current.getEndDate()
                ));
            }
        }
    }
}