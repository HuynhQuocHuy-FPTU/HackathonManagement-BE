package com.hackathon.controller;

import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.dto.event.EventResponse;
import com.hackathon.dto.event.UpdateEventRequest;
import com.hackathon.dto.expert.ExpertInfoResponse;
import com.hackathon.service.ExpertService;
import com.hackathon.service.ExpertServiceImpl;
import com.hackathon.service.event.EventService;
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

    @Autowired
    private ExpertService expertService;

    // =========================================================
    // PUBLIC ENDPOINTS (Dành cho Guest/Student/Admin)
    // =========================================================

    @GetMapping("/public")
    public ResponseEntity<List<EventResponse>> getPublicEvents() {
        return ResponseEntity.ok(eventService.getPublicEvents());
    }

    @GetMapping("/public/search")
    public ResponseEntity<List<EventResponse>> searchPublicEvents(@RequestParam String name) {
        return ResponseEntity.ok(eventService.searchPublicEvents(name));
    }

    @GetMapping("/detail/{eventId}")
    public ResponseEntity<EventResponse> getPublicEventDetail(@PathVariable Integer eventId) {
        return ResponseEntity.ok(eventService.getEventDetail(eventId));
    }


    // =========================================================
    // COORDINATOR ENDPOINTS (Dành cho quản trị viên)
    // =========================================================
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    @PutMapping("/publish/{eventId}")
    public ResponseEntity<String> publishEvent(@PathVariable Integer eventId){
        eventService.publishEvent(eventId);
        return ResponseEntity.ok("Sự kiện đã được công khai thành công");
    }
    @PutMapping("/delete/{eventId}")
    public ResponseEntity<String> deleteEvent(@PathVariable Integer eventId){
        eventService.deleteEvent(eventId);
        return ResponseEntity.ok("Sự kiện đã được xóa thành công và chuyển vào thùng rác");
    }
    @PutMapping("/update/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(@Valid @RequestBody UpdateEventRequest request, @PathVariable Integer eventId){
        EventResponse response = eventService.updateEvent(request, eventId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trash")
    public ResponseEntity<List<EventResponse>> getDeletedEvents(){
        List<EventResponse> responses = eventService.getDeletedEvents();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/restore/{eventId}")
    public ResponseEntity<String> restoreEvent(@PathVariable Integer eventId){
        eventService.restoreEvent(eventId);
        return ResponseEntity.ok("Khôi phục event thành công! Trạng thái đã được cập nhật");
    }

    @DeleteMapping("/permanently/{eventId}")
    public ResponseEntity<String> permanentlyDeleteEvent(@PathVariable Integer eventId){
        eventService.permanentlyDeleteEvent(eventId);
        return ResponseEntity.ok("Đã xóa vĩnh viễn event");
    }
    @GetMapping("/search")
    public ResponseEntity<List<EventResponse>> searchEvents(@RequestParam(required = false) String name) {

        return ResponseEntity.ok(eventService.searchByEventName(name));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventDetail(@PathVariable Integer eventId) {
        return ResponseEntity.ok(eventService.getEventDetail(eventId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<EventResponse>> getAllEvent(){
        return ResponseEntity.ok(eventService.getAllEvent());
    }

    @GetMapping("/experts")
    public ResponseEntity<List<ExpertInfoResponse>> getAllExperts(){
        return ResponseEntity.ok(expertService.getAllExperts());
    }

}
