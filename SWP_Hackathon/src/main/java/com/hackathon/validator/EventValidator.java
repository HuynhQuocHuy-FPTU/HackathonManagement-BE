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

    public void validatorCreate(CreateEventRequest request) throws BadRequestException {
        // 1. Kiểm tra tồn tại tên (Dù là draft cũng nên check tên nếu có nhập)
        if (request.getEventName() != null) {
            boolean exists = eventRepository.existsHackathonEventByEventName(request.getEventName());
            if (exists) {
                throw new BadRequestException("Tên sự kiện '" + request.getEventName() + "' đã tồn tại!");
            }
        }

        // 2. Kiểm tra thời gian (Chỉ check nếu có dữ liệu)
        LocalDateTime now = LocalDateTime.now();

        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new BadRequestException("Ngày bắt đầu sự kiện phải nằm trước ngày kết thúc!");
            }
        }

        if (request.getStartDate() != null && request.getStartDate().isBefore(now)) {
            throw new BadRequestException("Ngày bắt đầu sự kiện không được nằm trong quá khứ!");
        }

        if (request.getRegistrationDeadline() != null) {
            if (request.getRegistrationDeadline().isBefore(now)) {
                throw new BadRequestException("Hạn chót đăng ký không được nằm trong quá khứ!");
            }
            if (request.getStartDate() != null && request.getRegistrationDeadline().isAfter(request.getStartDate())) {
                throw new BadRequestException("Hạn chót đăng ký phải diễn ra trước ngày bắt đầu sự kiện!");
            }
        }

        // 3. Kiểm tra team size (Chỉ check nếu cả hai đều có dữ liệu)
        if (request.getMinTeamSize() != null && request.getMaxTeamSize() != null) {
            if (request.getMinTeamSize() > request.getMaxTeamSize()) {
                throw new BadRequestException("Số lượng thành viên tối thiểu không được lớn hơn tối đa!");
            }
        }
    }

    public void publishEventValidator(HackathonEvent event) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Kiểm tra trạng thái sự kiện
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Sự kiện này đã được công bố hoặc đã bị xóa trước đó!");
        }

        // 2. Kiểm tra NULL cho các trường thời gian BẮT BUỘC trước khi thực hiện so sánh
        if (event.getRegistrationDeadline() == null) {
            throw new BadRequestException("Hạn chót đăng ký không được để trống!");
        }
        if (event.getStartDate() == null || event.getEndDate() == null) {
            throw new BadRequestException("Ngày bắt đầu/kết thúc không được để trống!");
        }

        // 3. Bây giờ mới thực hiện so sánh thời gian (An toàn vì đã check null)
        if (now.isAfter(event.getRegistrationDeadline())) {
            throw new BadRequestException("Không thể công bố sự kiện! Thời gian hiện tại đã vượt quá hạn chót đăng ký của cuộc thi.");
        }

        if (event.getRegistrationDeadline().isAfter(event.getStartDate())) {
            throw new BadRequestException("Hạn chót đăng ký phải diễn ra trước ngày bắt đầu sự kiện.");
        }

        // 4. Các kiểm tra dữ liệu khác
        if (isNullOrBlank(event.getEventName())) throw new BadRequestException("Tên sự kiện không được để trống!");
        if (isNullOrBlank(event.getTitle())) throw new BadRequestException("Tiêu đề không được để trống!");
        if (isNullOrBlank(event.getAddress())) throw new BadRequestException("Địa chỉ không được để trống!");
        if (isNullOrBlank(event.getDescription())) throw new BadRequestException("Mô tả không được để trống!");

        if (event.getMaxTeam() == null || event.getMaxTeam() < 1)
            throw new BadRequestException("Số lượng đội thi không hợp lệ!");

        // 5. Kiểm tra danh sách Category và Round
        if (event.getCategories() == null || event.getCategories().isEmpty()) {
            throw new BadRequestException("Sự kiện phải có ít nhất một hạng mục (Category)!");
        }

        if (event.getRounds() == null || event.getRounds().isEmpty()) {
            throw new BadRequestException("Sự kiện phải có ít nhất một vòng thi (Round)!");
        }

        // 6. Kiểm tra chi tiết các Round
        for (var round : event.getRounds()) {
            if (isNullOrBlank(round.getRoundName()))
                throw new BadRequestException("Tên vòng thi không được để trống!");
            if (round.getStartTime() == null || round.getEndTime() == null)
                throw new BadRequestException("Thời gian vòng thi không được để trống!");
            if (round.getCriteriaSet() == null) {
                throw new BadRequestException("Vòng thi '" + round.getRoundName() + "' chưa chọn bộ tiêu chí chấm điểm (criteria set)!");
            }
            boolean hasAnyExpert = false;
            for (var categoryRound : round.getCategoryRounds()) {
                if (categoryRound.getExpertAssigns() != null && !categoryRound.getExpertAssigns().isEmpty()) {
                    hasAnyExpert = true;
                    break;
                }
            }
            if (!hasAnyExpert) {
                throw new BadRequestException("Vòng thi '" + round.getRoundName() + "' chưa được gán chuyên gia nào!");
            }
        }
    }

    private boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public void updateEventValidator(UpdateEventRequest request, HackathonEvent event) {

        // 1. Kiểm tra trạng thái (Luôn luôn check)
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Sự kiện này đã được công bố, không thể sửa đổi!");
        }

        LocalDateTime now = LocalDateTime.now();

        // 2. Kiểm tra thời gian (Chỉ validate nếu request có gửi dữ liệu lên)
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new BadRequestException("Ngày bắt đầu sự kiện phải nằm trước ngày kết thúc!");
            }
        }

        // Kiểm tra quá khứ (Chỉ check nếu startDate có giá trị)
        if (request.getStartDate() != null && request.getStartDate().isBefore(now)) {
            throw new BadRequestException("Ngày bắt đầu sự kiện không được nằm trong quá khứ!");
        }

        // Kiểm tra deadline
        if (request.getRegistrationDeadline() != null) {
            if (request.getRegistrationDeadline().isBefore(now)) {
                throw new BadRequestException("Hạn chót đăng ký không được nằm trong quá khứ!");
            }

            // Chỉ so sánh deadline với startDate nếu cả hai đều tồn tại
            if (request.getStartDate() != null && request.getRegistrationDeadline().isAfter(request.getStartDate())) {
                throw new BadRequestException("Hạn chót đăng ký phải diễn ra trước hoặc trùng với ngày bắt đầu sự kiện!");
            }
        }

        // 3. Kiểm tra team size (Chỉ check nếu cả hai đều có dữ liệu)
        if (request.getMinTeamSize() != null && request.getMaxTeamSize() != null) {
            if (request.getMinTeamSize() > request.getMaxTeamSize()) {
                throw new BadRequestException("Số lượng thành viên tối thiểu không được lớn hơn tối đa!");
            }
        }
    }
}


