package com.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class EventCoordinatorDTO {
    private Integer coordinatorId;
    private String coordinatorName;
    private String department;
    private String status;
    private Integer accountId;
    private String organizationId;


}
