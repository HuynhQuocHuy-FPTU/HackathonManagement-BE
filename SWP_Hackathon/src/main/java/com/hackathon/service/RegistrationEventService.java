package com.hackathon.service;

import com.hackathon.dto.registration.RegistrationResponse;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface RegistrationEventService {
    void registerEvent(Integer eventId, CustomUserDetails userDetails);

    List<RegistrationResponse> getTeamsForApproval(Integer evenId, CustomUserDetails userDetails);

    RegistrationResponse getTeamsDetailForApproval(Integer registrationId, CustomUserDetails userDetails);

}
