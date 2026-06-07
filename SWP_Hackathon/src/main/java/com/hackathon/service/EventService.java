package com.hackathon.service;

import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.exception.BadRequestException;

public interface EventService {
    public void createEvent(CreateEventRequest request) throws BadRequestException;

}
