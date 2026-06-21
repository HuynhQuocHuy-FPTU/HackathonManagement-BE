package com.hackathon.service;

import com.hackathon.dto.TeamSelectionDTO;
import com.hackathon.dto.registration.RegistrationResponse;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.entity.Registration;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface RegistrationEventService {
    void registerEvent(Integer eventId, CustomUserDetails userDetails);
    Registration approveRegistration(Integer registrationId);

    Registration rejectRegistration(Integer registrationId);

    List<TeamSelectionDTO> getApprovedRegistrations(Integer eventId);
    RegistrationResponse getTeamsDetailForApproval(Integer registrationId, CustomUserDetails userDetails);
    List<RegistrationResponse> getTeamsForApproval(Integer evenId, CustomUserDetails userDetails);
}
