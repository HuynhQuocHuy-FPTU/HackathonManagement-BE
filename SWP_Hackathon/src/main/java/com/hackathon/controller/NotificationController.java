package com.hackathon.controller;

import com.hackathon.dto.notification.NotiResponseRequest;
import com.hackathon.dto.notification.NotificationWebResponse;
import com.hackathon.entity.Notification;
import com.hackathon.entity.enums.NotificationType;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/web/all")
    public ResponseEntity<List<NotificationWebResponse>>getAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getNotifications(userDetails));
    }
    @GetMapping("/web/unread")
    public ResponseEntity<List<NotificationWebResponse>> getUnread(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userDetails));
    }
    @GetMapping("/web/read")
    public ResponseEntity<List<NotificationWebResponse>> getRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getReadNotifications(userDetails));
    }

    @GetMapping("/web/unread/count")
    public ResponseEntity<Long> countUnread(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.countUnread(userDetails));
    }

    @PutMapping("/web/{id}/read")
    public ResponseEntity<String> markAsRead(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAsRead(id, userDetails);
        return ResponseEntity.ok("Đã đọc notification");
    }

    @PutMapping("/web/read-all")
    public ResponseEntity<String> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.markAllAsRead(userDetails);
        return ResponseEntity.ok("Đã đọc tất cả notification");
    }
    @DeleteMapping("/web/{id}")
    public ResponseEntity<String> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.deleteNotification(id, userDetails);
        return ResponseEntity.ok("Đã xóa notification");
    }

    @GetMapping("/web/filter")
    public ResponseEntity<List<NotificationWebResponse>> getByType(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam NotificationType type
    ) {
        return ResponseEntity.ok(notificationService.getByType(userDetails, type));
    }

    @PostMapping("/web/response/{notiId}")
    public ResponseEntity<Void> responseCategoryAssigment(@PathVariable Long notiId, @RequestBody NotiResponseRequest request,@AuthenticationPrincipal CustomUserDetails userDetails){
        notificationService.responseCategoryAssignment(notiId, request.getMessage(),userDetails );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/web/pending-response")
    public ResponseEntity<List<NotificationWebResponse>> getPendingResponses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                notificationService.getPendingResponses(userDetails));
    }

}
