package com.hackathon.validator;

import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.dto.event.UpdateEventRequest;
import com.hackathon.dto.round.UpdateRoundRequest;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.HackathonEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EventValidator {
    @Autowired
    private HackathonEventRepository eventRepository;

    // Khoảng cách tối thiểu (đơn vị: ngày)
    private static final int MIN_GAP_REG_TO_START = 3;
    private static final int MIN_GAP_WORKSHOP_TO_START = 1;
    private static final int MIN_GAP_DEADLINE_TO_WORKSHOP = 1;

    // 1. Validator cho việc Tạo mới
    public void validatorCreate(CreateEventRequest request) throws BadRequestException {
        checkEventNameExists(request.getEventName());
        validateTimeLogic(request.getStartDate(), request.getEndDate(), request.getRegistrationDeadline(), request.getWorkshopTime(), LocalDateTime.now());
        validateTeamSize(request.getMinTeamSize(), request.getMaxTeamSize());
    }

    // 2. Validator cho việc Cập nhật
    public void updateEventValidator(UpdateEventRequest request, HackathonEvent event) throws BadRequestException {
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Sự kiện này đã được công bố, không thể sửa đổi!");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = (request.getStartDate() != null) ? request.getStartDate() : event.getStartDate();
        LocalDateTime end = (request.getEndDate() != null) ? request.getEndDate() : event.getEndDate();
        LocalDateTime deadline = (request.getRegistrationDeadline() != null) ? request.getRegistrationDeadline() : event.getRegistrationDeadline();
        LocalDateTime workshop = (request.getWorkshopTime() != null) ? request.getWorkshopTime() : event.getWorkshopTime();

        validateTimeLogic(start, end, deadline, workshop, now);
        validateTeamSize(request.getMinTeamSize() != null ? request.getMinTeamSize() : event.getMinTeamSize(),
                request.getMaxTeamSize() != null ? request.getMaxTeamSize() : event.getMaxTeamSize());
    }

    // 3. Validator cho việc Công bố (Publish)
    public void publishEventValidator(HackathonEvent event) throws BadRequestException {
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Sự kiện này đã được công bố hoặc đã bị xóa!");
        }

        validateRequiredFields(event);
        validateTimeLogic(event.getStartDate(), event.getEndDate(), event.getRegistrationDeadline(), event.getWorkshopTime(), LocalDateTime.now());
        validateStructure(event);
    }

    // --- CÁC HÀM BỔ TRỢ (PRIVATE HELPERS) ---

    private void validateTimeLogic(LocalDateTime start, LocalDateTime end, LocalDateTime deadline, LocalDateTime workshop, LocalDateTime now) {
        if (start != null && end != null && start.isAfter(end))
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc!");
        if (start != null && start.isBefore(now))
            throw new BadRequestException("Ngày bắt đầu không được nằm trong quá khứ!");

        if (deadline != null) {
            if (deadline.isBefore(now)) throw new BadRequestException("Hạn chót đăng ký không được nằm trong quá khứ!");
            if (start != null && deadline.isAfter(start.minusDays(MIN_GAP_REG_TO_START))) {
                throw new BadRequestException("Hạn chót đăng ký phải trước ngày bắt đầu ít nhất " + MIN_GAP_REG_TO_START + " ngày!");
            }
        }

        if (workshop != null) {
            if (workshop.isBefore(now)) throw new BadRequestException("Workshop không được nằm trong quá khứ!");
            if (deadline != null && workshop.isBefore(deadline.plusDays(MIN_GAP_DEADLINE_TO_WORKSHOP))) {
                throw new BadRequestException("Workshop phải sau hạn chót đăng ký ít nhất " + MIN_GAP_DEADLINE_TO_WORKSHOP + " ngày!");
            }
            if (start != null && workshop.isAfter(start.minusDays(MIN_GAP_WORKSHOP_TO_START))) {
                throw new BadRequestException("Workshop phải trước ngày bắt đầu ít nhất " + MIN_GAP_WORKSHOP_TO_START + " ngày!");
            }
        }
    }

    private void validateRequiredFields(HackathonEvent event) {
        if (isNullOrBlank(event.getEventName())) throw new BadRequestException("Tên sự kiện trống!");
        if (isNullOrBlank(event.getTitle())) throw new BadRequestException("Tiêu đề trống!");
        if (isNullOrBlank(event.getAddress())) throw new BadRequestException("Địa chỉ trống!");
        if (event.getDescription() == null) throw new BadRequestException("Mô tả trống!");
        if (event.getMaxTeam() == null || event.getMaxTeam() < 1) throw new BadRequestException("Số lượng đội thi không hợp lệ!");
    }

    private void validateStructure(HackathonEvent event) {
        if (event.getCategories() == null || event.getCategories().isEmpty()) throw new BadRequestException("Thiếu hạng mục!");
        if (event.getRounds() == null || event.getRounds().isEmpty()) throw new BadRequestException("Thiếu vòng thi!");

        for (var round : event.getRounds()) {
            if (isNullOrBlank(round.getRoundName())) throw new BadRequestException("Tên vòng thi trống!");
            if (round.getStartTime() == null || round.getEndTime() == null) throw new BadRequestException("Thời gian vòng thi trống!");
            if (round.getCriteriaSet() == null) throw new BadRequestException("Vòng thi '" + round.getRoundName() + "' chưa chọn bộ tiêu chí!");

            boolean hasExpert = round.getCategoryRounds().stream()
                    .anyMatch(cr -> cr.getExpertAssigns() != null && !cr.getExpertAssigns().isEmpty());
            if (!hasExpert) throw new BadRequestException("Vòng thi '" + round.getRoundName() + "' chưa được gán chuyên gia!");
        }
    }

    private void validateTeamSize(Integer min, Integer max) {
        if (min != null && max != null && min > max) throw new BadRequestException("Số lượng thành viên tối thiểu không được lớn hơn tối đa!");
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


