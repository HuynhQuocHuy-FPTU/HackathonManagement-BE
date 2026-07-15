package com.hackathon.service.event;

import com.hackathon.dto.event.CreateEventRequest;

import com.hackathon.dto.event.EventResponse;
import com.hackathon.dto.event.UpdateEventRequest;
import com.hackathon.exception.BadRequestException;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface EventService {

    public EventResponse createEvent(CreateEventRequest request) throws BadRequestException;
    public void publishEvent(Integer eventID);
    public EventResponse updateEvent(UpdateEventRequest request, Integer eventId);
    public void deleteEvent(Integer eventID);
    public List<EventResponse> getDeletedEvents();
    public void restoreEvent(Integer eventId);
    public void permanentlyDeleteEvent(Integer eventId);
    void cancelEvent(Integer eventId, String reason, CustomUserDetails currentUser);
    // Information about HackathonEvent Detail
    public EventResponse  getEventDetail(Integer eventID);
    //Search HackathonEvent by Name
    public List<EventResponse>searchByEventName(String eventName);
    //General Information about HackathonEVent
    public List<EventResponse> getAllEvent();

    public List<EventResponse> getPublicEvents();

    public List<EventResponse> searchPublicEvents(String eventName);
}
