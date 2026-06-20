package com.hackathon.service;

import com.hackathon.entity.Registration;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    public Registration findById(Integer registrationId){
        Registration registration = registrationRepository.findByRegistrationId(registrationId).orElseThrow(() -> new BadRequestException("Không tìm thấy Registration với id: " + registrationId));
        return registration;
    }
}
