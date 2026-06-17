package com.hackathon.controller;

import com.hackathon.dto.event.EventResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.service.event.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/v1/registration")
public class RegistrationController {
    @Autowired
    private EventService eventService;

    //View General Information about hackathon
    @GetMapping("/event")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAllEvent() {
        List<EventResponse> list = eventService.getAllEvent();
        return ResponseEntity.ok(ApiResponse.success(list, "Get all events successfully"));
    }


    //View all information detail about hackathon(click Event show details)
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventDetail(@PathVariable("eventId") Integer id) {
        EventResponse  list = eventService.getEventDetail(id);
        return ResponseEntity.ok(ApiResponse.success(list, "Get all event details successfully"));
    }

    //Search HackathonEvent By EventName
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<EventResponse>>> searchHackathonEvent(@RequestParam("eventName") String name) {
        List<EventResponse> list = eventService.searchByEventName(name);
        return ResponseEntity.ok(ApiResponse.success(list, "Search Successfully"));
    }


}
