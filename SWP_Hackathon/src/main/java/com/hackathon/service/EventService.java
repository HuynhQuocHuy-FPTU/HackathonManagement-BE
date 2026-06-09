package com.hackathon.service;

import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.dto.event.EventResponse;
import com.hackathon.dto.event.UpdateEventRequest;
import com.hackathon.exception.BadRequestException;
import jakarta.persistence.criteria.CriteriaBuilder;

import java.util.List;

public interface EventService {

    public EventResponse createEvent(CreateEventRequest request) throws BadRequestException;
    public void publishEvent(Integer eventID);
    public void updateEvent(UpdateEventRequest request);
    public void deleteEvent(Integer eventID);
    public List<EventResponse> getDeletedEvents();
    public void restoreEvent(Integer eventId);
    public void permanentlyDeleteEvent(Integer eventId);
}
