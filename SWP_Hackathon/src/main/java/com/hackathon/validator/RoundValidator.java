package com.hackathon.validator;

import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.exception.BadRequestException;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class RoundValidator {
    public void validatorCreate(CreateRoundRequest request) throws BadRequestException {

        //check time
        if(request.getStartDate().isAfter(request.getEndDate())){
            throw new BadRequestException("Start date must be before end date");
        }




    }
}
