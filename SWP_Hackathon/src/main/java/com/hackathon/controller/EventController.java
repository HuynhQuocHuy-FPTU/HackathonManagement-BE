package com.hackathon.controller;

import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/events")
public class EventController {
    @Autowired
    private EventService eventService;
    @PostMapping
    public ResponseEntity<String> createEvent(@Valid @RequestBody CreateEventRequest request) {
        eventService.createEvent(request);
        return  ResponseEntity.ok("Create event successfully");
    }

}
