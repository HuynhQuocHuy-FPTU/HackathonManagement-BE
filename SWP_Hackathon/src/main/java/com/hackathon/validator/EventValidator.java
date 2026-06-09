package com.hackathon.validator;

import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EventValidator {

    public void validatorCreate(CreateEventRequest request) throws BadRequestException {
        //check time
        if(request.getStartDate().isAfter(request.getEndDate())){
            throw new BadRequestException("Start date must be before end date");


        }

        // check time
        if(request.getStartDate().isBefore(LocalDateTime.now())){
            throw new BadRequestException("Start date cannot be in the past");
        }

        if(request.getRegistrationDeadline().isBefore(request.getStartDate())){
            throw new BadRequestException("Registration deadline must be after start date");
        }

        // check team size
        if (request.getMinTeamSize() > request.getMaxTeamSize()) {
            throw new BadRequestException("Min team size cannot be greater than max team size");
        }

        //
    }
}
