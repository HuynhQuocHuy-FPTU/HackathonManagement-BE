package com.hackathon.controller;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.dto.event.EventResponse;
import com.hackathon.dto.event.UpdateEventRequest;
import com.hackathon.dto.event.UpdateTimeEventDTO;
import com.hackathon.dto.expert.ExpertInfoResponse;
import com.hackathon.dto.round.UpdateTimeRoundRequest;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.CategoryService;
import com.hackathon.service.ExpertService;
import com.hackathon.service.RoundService;
import com.hackathon.service.impl.StatusSchedulerService;
import com.hackathon.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private RoundService roundService;
    @Autowired
    private StatusSchedulerService statusSchedulerService;

    // =========================================================
    // PUBLIC (Dành cho Guest/Student/Admin)
    // =========================================================

    @GetMapping("/public")
    public ResponseEntity<List<EventResponse>> getPublicEvents(
            @RequestParam(required = false) Integer year
    ) {
        if (year == null) {
            return ResponseEntity.ok(eventService.getPublicEvents());
        }
        return ResponseEntity.ok(eventService.getPublicEventsByYear(year));
    }

    @GetMapping("/public/years")
    public ResponseEntity<List<Integer>> getPublicEventYears() {
        return ResponseEntity.ok(eventService.getPublicEventYears());
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
        return ResponseEntity.ok(ApiResponse.success(response, "Đã tạo cuộc thi thành công"));
    }

    @PutMapping("/publish/{eventId}")
    public ResponseEntity<ApiResponse<Void>> publishEvent(@PathVariable Integer eventId) {

        eventService.publishEvent(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Cuộc thi đã được công khai thành công")
        );
    }
    @PutMapping("/delete/{eventId}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable Integer eventId) {

        eventService.deleteEvent(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Cuộc thi đã được xóa thành công và chuyển vào thùng rác")
        );
    }
    @PutMapping("/update/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @Valid @RequestBody UpdateEventRequest request,
            @PathVariable Integer eventId
    ) {
        EventResponse response = eventService.updateEvent(request, eventId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Cập nhật cuộc thi thành công")
        );
    }
    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getDeletedEvents() {

        List<EventResponse> responses = eventService.getDeletedEvents();

        return ResponseEntity.ok(
                ApiResponse.success(responses, "Danh sách cuộc thi đã xóa")
        );
    }

    @PutMapping("/restore/{eventId}")
    public ResponseEntity<ApiResponse<Void>> restoreEvent(@PathVariable Integer eventId) {

        eventService.restoreEvent(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Khôi phục cuộc thi thành công")
        );
    }

    @DeleteMapping("/permanently/{eventId}")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteEvent(@PathVariable Integer eventId) {

        eventService.permanentlyDeleteEvent(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Đã xóa vĩnh viễn cuộc thi")
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
                ApiResponse.success(eventService.getEventDetail(eventId), "Chi tiết cuộc thi")
        );
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAllEvent(
            @RequestParam(required = false) Integer year
    ) {

        List<EventResponse> events = year == null
                ? eventService.getAllEvent()
                : eventService.getAllEventsByYear(year);

        return ResponseEntity.ok(
                ApiResponse.success(events, "Danh sách cuộc thi")
        );
    }

    @GetMapping("/years")
    public ResponseEntity<ApiResponse<List<Integer>>> getAllEventYears() {
        return ResponseEntity.ok(
                ApiResponse.success(eventService.getAllEventYears(), "Danh sách năm tổ chức sự kiện")
        );
    }

    @PostMapping("/check-minimum-teams")
    public ResponseEntity<ApiResponse<Void>> checkMinimumTeams() {
        statusSchedulerService.checkAndCancelEventsBelowMinimumTeams();
        return ResponseEntity.ok(
                ApiResponse.success(null, "Đã kiểm tra số đội tối thiểu của các sự kiện hết hạn đăng ký")
        );
    }

    @GetMapping("/experts")
    public ResponseEntity<ApiResponse<List<ExpertInfoResponse>>> getAllExperts() {

        return ResponseEntity.ok(
                ApiResponse.success(expertService.getAllExperts(), "Danh sách chuyên gia")
        );
    }
    @GetMapping("/categories/{eventId}")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoriesOfEvent(@PathVariable Integer eventId){
        List<CategoryResponse> categoryResponses = categoryService.getAllCategories(eventId);
        return ResponseEntity.ok(ApiResponse.success(categoryResponses, "Các hạng mục thuộc về cuộc thi"));
    }

    @PutMapping("/cancel/{eventId}")
    public ResponseEntity<ApiResponse<Void>> cancelledEvent(
            @PathVariable Integer eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String reason
    ) {
        eventService.cancelEvent(eventId, reason, userDetails);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Đã hủy cuộc thi và gửi thông báo đến các đội thi")
        );
    }

    // =========================================================
    // UPDATE ROUND TIME
    // =========================================================

    @PutMapping("/update-time-round/{roundId}")
    public ResponseEntity<ApiResponse<Void>> updateTimeRound(
            @PathVariable Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdateTimeRoundRequest request
    ) {
        roundService.updateTimeRound(request,userDetails, roundId);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Đã chỉnh sửa thời gian của vòng thi thành công")
        );
    }

    // =========================================================
    // UPDATE EVENT TIME
    // =========================================================

    @PutMapping("/update-time-event/{eventId}")
    public ResponseEntity<ApiResponse<Void>> updateTimeEvent(
            @PathVariable Integer eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdateTimeEventDTO updateTimeEventDTO
            ) {
        eventService.updateTimeEvent(userDetails,eventId,updateTimeEventDTO);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Đã chỉnh sửa thời gian của event thành công")
        );
    }




}
