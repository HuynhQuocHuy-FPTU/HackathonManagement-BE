package com.hackathon.dto.registration;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class CountRegistrationDTO {
    private Integer countApproved;

    private Integer countReject;

    private Integer countPending;
}
