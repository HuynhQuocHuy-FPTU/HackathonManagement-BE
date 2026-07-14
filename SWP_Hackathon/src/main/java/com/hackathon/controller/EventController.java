package com.hackathon.controller;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.dto.event.EventResponse;
import com.hackathon.dto.event.UpdateEventRequest;
import com.hackathon.dto.expert.ExpertInfoResponse;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.CategoryService;
import com.hackathon.service.ExpertService;
import com.hackathon.service.RegistrationEventService;
import com.hackathon.service.event.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/events")
public class EventController {
    @Autowired
    private EventService eventService;
    @Autowired
    private ExpertService expertService;
    @Autowired
    private CategoryService categoryService;

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

    @GetMapping("/public/detail/{eventId}")
    public ResponseEntity<EventResponse> getPublicEventDetail(@PathVariable Integer eventId) {
        return ResponseEntity.ok(eventService.getEventDetail(eventId));
    }


    // =========================================================
    // COORDINATOR ENDPOINTS (Dành cho quản trị viên)
    // =========================================================
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã tạo event thành công"));
    }

    @PutMapping("/publish/{eventId}")
    public ResponseEntity<ApiResponse<Void>> publishEvent(@PathVariable Integer eventId) {

        eventService.publishEvent(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Sự kiện đã được công khai thành công")
        );
    }
    @PutMapping("/delete/{eventId}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable Integer eventId) {

        eventService.deleteEvent(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Sự kiện đã được xóa thành công và chuyển vào thùng rác")
        );
    }
    @PutMapping("/update/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @Valid @RequestBody UpdateEventRequest request,
            @PathVariable Integer eventId
    ) {
        EventResponse response = eventService.updateEvent(request, eventId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Cập nhật sự kiện thành công")
        );
    }
    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getDeletedEvents() {

        List<EventResponse> responses = eventService.getDeletedEvents();

        return ResponseEntity.ok(
                ApiResponse.success(responses, "Danh sách sự kiện đã xóa")
        );
    }

    @PutMapping("/restore/{eventId}")
    public ResponseEntity<ApiResponse<Void>> restoreEvent(@PathVariable Integer eventId) {

        eventService.restoreEvent(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Khôi phục event thành công")
        );
    }

    @DeleteMapping("/permanently/{eventId}")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteEvent(@PathVariable Integer eventId) {

        eventService.permanentlyDeleteEvent(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Đã xóa vĩnh viễn event")
        );
    }
    @GetMapping("/search-all")
    public ResponseEntity<ApiResponse<List<EventResponse>>> searchEvents(
            @RequestParam(required = false) String name
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(eventService.searchByEventName(name), "Kết quả tìm kiếm")
        );
    }

    @GetMapping("/detail/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventDetail(@PathVariable Integer eventId) {

        return ResponseEntity.ok(
                ApiResponse.success(eventService.getEventDetail(eventId), "Chi tiết event")
        );
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAllEvent() {

        return ResponseEntity.ok(
                ApiResponse.success(eventService.getAllEvent(), "Danh sách event")
        );
    }

    @GetMapping("/experts")
    public ResponseEntity<ApiResponse<List<ExpertInfoResponse>>> getAllExperts() {

        return ResponseEntity.ok(
                ApiResponse.success(expertService.getAllExperts(), "Danh sách expert")
        );
    }
    @GetMapping("/categories/{eventId}")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoriesOfEvent(@PathVariable Integer eventId){
        List<CategoryResponse> categoryResponses = categoryService.getAllCategories(eventId);
        return ResponseEntity.ok(ApiResponse.success(categoryResponses, "Các categories thuộc về event"));
    }

    @PutMapping("/cancel/{eventId}")
    public ResponseEntity<ApiResponse<Void>> cancelledEvent(
            @PathVariable Integer eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String reason
    ) {
        eventService.cancelEvent(eventId, reason, userDetails);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Đã hủy cuộc thi và gửi thông báo đến các team")
        );
    }




}
