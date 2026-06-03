package com.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ExpertDTO {
    private Integer expertId;
    private String expertName;
    private Integer accountId;
    private String organizationId;

}
