package com.hackathon.validator;

import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.dto.event.UpdateEventRequest;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.SystemConfigKey;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EventValidator {
    @Autowired
    private HackathonEventRepository eventRepository;
    @Autowired
    private RoundValidator roundValidator;
    @Autowired
    private SystemConfigService systemConfigService;

    // 1. Validator cho việc Tạo mới
    public void validatorCreate(CreateEventRequest request) throws BadRequestException {
        checkEventNameExists(request.getEventName());
        validateTimeLogic(
                request.getStartDate(),
                request.getEndDate(),
                request.getRegistrationDeadline(),
                request.getWorkshopTime(),
                LocalDateTime.now(),
                false
        );
        validateTeamSize(request.getMinTeamSize(), request.getMaxTeamSize());
        validateTeamCount(request.getMinTeam(), request.getMaxTeam());
    }

    // 2. Validator cho việc Cập nhật
    public void updateEventValidator(UpdateEventRequest request, HackathonEvent event) throws BadRequestException {
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Sự kiện này đã được công bố, không thể sửa đổi!");
        }

        validateTimeLogic(
                request.getStartDate(),
                request.getEndDate(),
                request.getRegistrationDeadline(),
                request.getWorkshopTime(),
                LocalDateTime.now(),
                true
        );
        validateTeamSize(
                request.getMinTeamSize(),
                request.getMaxTeamSize()
        );
        validateTeamCount(request.getMinTeam(), request.getMaxTeam());
    }

    // 3. Validator cho việc Công bố (Publish)
    public void publishEventValidator(HackathonEvent event) throws BadRequestException {
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Sự kiện này đã được công bố hoặc đã bị xóa!");
        }

        validateRequiredFields(event);
        validateTimeLogic(
                event.getStartDate(),
                event.getEndDate(),
                event.getRegistrationDeadline(),
                event.getWorkshopTime(),
                LocalDateTime.now(),
                true
        );
        validateStructure(event);
    }



    public void validateTimeLogic(LocalDateTime start, LocalDateTime end, LocalDateTime deadline, LocalDateTime workshop, LocalDateTime now, boolean validatePast
    ) {
        if (start != null && end != null && start.isAfter(end))
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc!");
        if (validatePast && start != null && start.isBefore(now))
            throw new BadRequestException("Ngày bắt đầu không được nằm trong quá khứ!");

        if (deadline != null) {
            if (validatePast && deadline.isBefore(now))
                throw new BadRequestException("Hạn chót đăng ký không được nằm trong quá khứ!");

            if(deadline.isAfter(start)){
                throw new BadRequestException("Hạn chót đăng ký phải trước ngày bắt đầu diễn ra sự kiện");
            }
        }

        if (workshop != null) {
            if (validatePast && workshop.isBefore(now))
                throw new BadRequestException("buổi bóc thăm không được nằm trong quá khứ!");

            if(deadline != null && workshop.isBefore(deadline)){
                throw new BadRequestException("Ngày diễn ra bóc thăm không được bắt đầu trước ngày kết thúc đăng kí tham gia");
            }

            if (start != null && workshop.isAfter(start)) {
                throw new BadRequestException("buổi bóc thăm phải trước ngày bắt đầu sự kiện");
            }

        }
    }

    public void validateEventEndAfterRounds(
            LocalDateTime eventEndTime,
            HackathonEvent event
    ) {
        if (eventEndTime == null) {
            return;
        }

        if (event.getRounds() == null) {
            return;
        }

        for (var round : event.getRounds()) {
            if (round.getEndTime() != null
                    && eventEndTime.isBefore(round.getEndTime())) {
                throwEventEndsBeforeRound(
                        eventEndTime,
                        round.getRoundName(),
                        round.getEndTime()
                );
            }
        }
    }

    private void throwEventEndsBeforeRound(
            LocalDateTime eventEndTime,
            String roundName,
            LocalDateTime roundEndTime
    ) {
        throw new BadRequestException(
                "Không thể kết thúc sự kiện lúc " + eventEndTime
                        + " vì vòng '" + roundName
                        + "' vẫn còn diễn ra đến " + roundEndTime
        );
    }
    // --- CÁC HÀM BỔ TRỢ (PRIVATE HELPERS) ---

    /**
     * Thời gian kết thúc mới của event không được sớm hơn thời gian kết thúc
     * của bất kỳ round nào thuộc event.
     *
     * Validation này chỉ được gọi trong API cập nhật riêng thời gian event,
     * không chạy trong API update thông tin event thông thường.
     */


    private void validateRequiredFields(HackathonEvent event) {
        if (isNullOrBlank(event.getEventName())) throw new BadRequestException("Tên sự kiện trống!");
        if (isNullOrBlank(event.getTitle())) throw new BadRequestException("Tiêu đề trống!");
        if (isNullOrBlank(event.getAddress())) throw new BadRequestException("Địa chỉ trống!");
        if (event.getDescription() == null) throw new BadRequestException("Mô tả trống!");
        if (event.getSeason() == null) throw new BadRequestException("Mùa tổ chức sự kiện trống!");
        if (event.getSeasonYear() == null) throw new BadRequestException("Năm tổ chức sự kiện trống!");
        if (event.getMaxTeam() == null || event.getMaxTeam() < 1) throw new BadRequestException("Số lượng đội thi không hợp lệ!");
        if (event.getMinTeam() == null || event.getMinTeam() < 1) throw new BadRequestException("Số lượng đội tối thiểu không hợp lệ!");
        validateTeamCount(event.getMinTeam(), event.getMaxTeam());
        if (event.getStartDate() == null) throw new BadRequestException("Ngày bắt đầu sự kiện trống!");
        if (event.getEndDate() == null) throw new BadRequestException("Ngày kết thúc sự kiện trống!");
        if (event.getRegistrationDeadline() == null) throw new BadRequestException("Hạn chót đăng ký trống!");
        if (event.getWorkshopTime() == null) throw new BadRequestException("Thời gian workshop trống!");
    }

    private void validateStructure(HackathonEvent event) {
        if (event.getCategories() == null || event.getCategories().isEmpty()) throw new BadRequestException("Thiếu hạng mục!");
        if (event.getRounds() == null || event.getRounds().isEmpty()) throw new BadRequestException("Thiếu vòng thi!");

        for (var round : event.getRounds()) {
            if (isNullOrBlank(round.getRoundName())) throw new BadRequestException("Tên vòng thi trống!");
            if (round.getStartTime() == null || round.getEndTime() == null) throw new BadRequestException("Thời gian vòng thi trống!");
            if (round.getCriteriaSet() == null) throw new BadRequestException("Vòng thi '" + round.getRoundName() + "' chưa chọn bộ tiêu chí!");
            roundValidator.validateRoundForPublish(round, event);

            boolean hasExpert = round.getCategoryRounds().stream()
                    .anyMatch(cr -> cr.getExpertAssigns() != null && !cr.getExpertAssigns().isEmpty());
            if (!hasExpert) throw new BadRequestException("Vòng thi '" + round.getRoundName() + "' chưa được gán chuyên gia!");
        }
    }

    private void validateTeamSize(Integer min, Integer max) {
        if (min != null && max != null && min > max) throw new BadRequestException("Số lượng thành viên tối thiểu không được lớn hơn tối đa!");

        if(max > systemConfigService.getIntConfig(SystemConfigKey.MAX_TEAM_SIZE)){
            throw new BadRequestException("Số lượng thành viên tối đa không được vượt quá " + systemConfigService.getIntConfig(SystemConfigKey.MAX_TEAM_SIZE));
        }
    }

    private void validateTeamCount(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new BadRequestException("Số lượng đội tối thiểu không được lớn hơn số lượng đội tối đa!");
        }
    }

    private void checkEventNameExists(String name) {
        if (name != null && eventRepository.existsHackathonEventByEventName(name)) {
            throw new BadRequestException("Tên sự kiện '" + name + "' đã tồn tại!");
        }
    }

    private boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}


