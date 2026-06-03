package com.hackathon.dto;

import com.hackathon.entity.enums.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class OrganizationDTO {
    private String organizationId;
    private String address;
    private String emailDomain;
    private String shortName;
    private OrganizationType type;
    private String organizationName;

}
