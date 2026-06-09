package com.hackathon.service;

import com.hackathon.dto.event.request.CreateEventRequest;
import com.hackathon.dto.event.response.EventDetailResponse;
import com.hackathon.dto.event.response.EventResponse;
import com.hackathon.exception.BadRequestException;

import java.util.List;

public interface EventService {
    public void createEvent(CreateEventRequest request) throws BadRequestException;
    // Information about HackathonEvent Detail
    public List<EventDetailResponse> getAllEventDetail(Integer eventID);
    //Search HackathonEvent by Name
    public List<EventResponse>searchByEventName(String eventName);
    //General Information about HackathonEVent
    public List<EventResponse> getAllEvent();
}
