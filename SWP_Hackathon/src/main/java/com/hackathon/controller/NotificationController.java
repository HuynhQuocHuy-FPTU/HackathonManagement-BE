package com.hackathon.controller;

import com.hackathon.dto.notification.NotiResponseRequest;
import com.hackathon.dto.notification.NotificationWebResponse;
import com.hackathon.dto.notification.ResponseEntry;
import com.hackathon.dto.team.TeamRequestResponse;
import com.hackathon.entity.enums.NotificationType;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.NotificationService;
import com.hackathon.service.TeamRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final TeamRequestService teamRequestService;

    @GetMapping("/web/all")
    public ResponseEntity<ApiResponse<List<NotificationWebResponse>>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.getNotifications(userDetails),
                        "Danh sách thông báo"
                )
        );
    }
    @GetMapping("/web/unread")
    public ResponseEntity<ApiResponse<List<NotificationWebResponse>>> getUnread(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.getUnreadNotifications(userDetails),
                        "Danh sách chưa đọc"
                )
        );
    }
    @GetMapping("/web/read")
    public ResponseEntity<ApiResponse<List<NotificationWebResponse>>> getRead(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.getReadNotifications(userDetails),
                        "Danh sách đã đọc"
                )
        );
    }

    @GetMapping("/web/unread/count")
    public ResponseEntity<ApiResponse<Long>> countUnread(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.countUnread(userDetails),
                        "Số thông báo chưa đọc"
                )
        );
    }

    @PutMapping("/web/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.markAsRead(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Đã đánh dấu đã đọc")
        );
    }

    @PutMapping("/web/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.markAllAsRead(userDetails);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Đã đọc tất cả thông báo")
        );
    }

    @DeleteMapping("/web/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.deleteNotification(id, userDetails);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Đã xóa thông báo")
        );
    }

    @GetMapping("/web/filter")
    public ResponseEntity<ApiResponse<List<NotificationWebResponse>>> getByType(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam NotificationType type
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.getByType(userDetails, type),
                        "Kết quả lọc theo loại"
                )
        );
    }

    @PostMapping("/web/response/{notiId}")
    public ResponseEntity<ApiResponse<TeamRequestResponse>> responseNotification(
            @PathVariable Long notiId,
            @Valid @RequestBody NotiResponseRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        TeamRequestResponse response = teamRequestService.respondNotification(
                userDetails, notiId, request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Phản hồi thành công")
        );
    }
    @GetMapping("/web/pending-response")
    public ResponseEntity<ApiResponse<List<ResponseEntry>>> getPendingResponses(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.getPendingResponses(userDetails),
                        "Danh sách chờ phản hồi"
                )
        );
    }

}
