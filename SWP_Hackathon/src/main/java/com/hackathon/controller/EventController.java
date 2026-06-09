package com.hackathon.controller;

import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.dto.event.EventResponse;
import com.hackathon.dto.event.UpdateEventRequest;
import com.hackathon.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/events")
public class EventController {
    @Autowired
    private EventService eventService;
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    @PatchMapping("/{eventId}/publish")
    public ResponseEntity<String> publishEvent(@PathVariable Integer eventId){
        eventService.publishEvent(eventId);
        return ResponseEntity.ok("Sự kiện đã được công khai thành công");
    }
    @PatchMapping("/{eventId}/delete")
    public ResponseEntity<String> deleteEvent(@PathVariable Integer eventId){
        eventService.deleteEvent(eventId);
        return ResponseEntity.ok("Sự kiện đã được xóa thành công và chuyển vào thùng rác");
    }
    @PutMapping("/{eventId}/update")
    public ResponseEntity<String> updateEvent(@Valid @RequestBody UpdateEventRequest request){
        eventService.updateEvent(request);
        return ResponseEntity.ok("Sự kiện đã được update thành công");
    }

    @GetMapping("/trash")
    public ResponseEntity<List<EventResponse>> getDeletedEvents(){
        List<EventResponse> responses = eventService.getDeletedEvents();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{eventId}/restore")
    public ResponseEntity<String> restoreEvent(@PathVariable Integer eventId){
        eventService.restoreEvent(eventId);
        return ResponseEntity.ok("Khôi phục event thành công! Trạng thái đã được cập nhật");
    }

    @DeleteMapping("/{eventId}/permanently")
    public ResponseEntity<String> permanentlyDeleteEvent(@PathVariable Integer eventId){
        eventService.permanentlyDeleteEvent(eventId);
        return ResponseEntity.ok("Đã xóa vĩnh viễn event");
    }

}
