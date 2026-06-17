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
        boolean exists = eventRepository.existsHackathonEventByEventName(request.getEventName());

        if (exists) {
            throw new BadRequestException("Tên sự kiện '" + request.getEventName() + "' đã tồn tại, vui lòng chọn tên khác!");
        }

        //check time
        if(request.getStartDate().isAfter(request.getEndDate())){
            throw new BadRequestException("Ngày bắt đầu event phải nằm trước ngày kết thúc event");
        }

        // check time
        if(request.getStartDate().isBefore(LocalDateTime.now())){
            throw new BadRequestException("Ngày bắt đầu event không được nằm trong quá khứ");
        }
        if (request.getRegistrationDeadline().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Hạn chót đăng ký không được nằm trong quá khứ");
        }

        if (request.getRegistrationDeadline().isAfter(request.getStartDate())) {
            throw new BadRequestException("Hạn chót đăng ký phải diễn ra trước hoặc trùng với ngày bắt đầu sự kiện");
        }

        // check team size
        if (request.getMinTeamSize() > request.getMaxTeamSize()) {
            throw new BadRequestException("Số lượng thành viên tối thiểu không được lớn hơn số lượng tối đa");
        }

        //
    }

    public void publishEventValidator(HackathonEvent event){
        LocalDateTime now = LocalDateTime.now();

        // Phải là bản nháp mới cho công bố
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Sự kiện này đã được công bố hoặc đã bị xóa trước đó!");
        }

        // Chặn trường hợp Admin ngâm Draft quá lâu, đến lúc bấm công bố thì đã lố hạn đăng ký
        if (now.isAfter(event.getRegistrationDeadline())) {
            throw new BadRequestException("Không thể công bố sự kiện! Thời gian hiện tại đã vượt quá hạn chót đăng ký của cuộc thi. Vui lòng cập nhật gia hạn lại thời gian đăng ký trước.");
        }

        // Hạn đăng ký phải nằm trước ngày bắt đầu cuôc thi
        if (event.getRegistrationDeadline().isAfter(event.getStartDate())) {
            throw new BadRequestException("Hạn chót đăng ký phải diễn ra trước ngày bắt đầu sự kiện.");
        }
    }
    public void updateEventValidator(UpdateEventRequest request, HackathonEvent event){

        if(event.getStatus() != EventStatus.DRAFT){
            throw new BadRequestException("Sự kiện này đã được công bố");
        }

        //check time
        if(request.getStartDate().isAfter(request.getEndDate())){
            throw new BadRequestException("Ngày bắt đầu event phải nằm trước ngày kết thúc event");
        }

        // check time
        if(request.getStartDate().isBefore(LocalDateTime.now())){
            throw new BadRequestException("Ngày bắt đầu event không được nằm trong quá khứ");
        }
        if (request.getRegistrationDeadline().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Hạn chót đăng ký không được nằm trong quá khứ");
        }

        if (request.getRegistrationDeadline().isAfter(request.getStartDate())) {
            throw new BadRequestException("Hạn chót đăng ký phải diễn ra trước hoặc trùng với ngày bắt đầu sự kiện");
        }

        // check team size
        if (request.getMinTeamSize() > request.getMaxTeamSize()) {
            throw new BadRequestException("Số lượng thành viên tối thiểu không được lớn hơn số lượng tối đa");
        }
    }

}
