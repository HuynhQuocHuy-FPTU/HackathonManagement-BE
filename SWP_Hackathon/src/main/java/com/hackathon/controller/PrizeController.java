package com.hackathon.controller;

import com.hackathon.dto.event.PrizeRequestDTO;
import com.hackathon.dto.event.PrizeResponseDTO;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.impl.PrizeServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prize")
public class PrizeController {
    private final PrizeServiceImpl prizeService;

    @PostMapping("/{eventId}/assign")
    public ResponseEntity<ApiResponse<Void>> assignPrize(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer eventId,
            @RequestBody(required = false) List<PrizeRequestDTO> request) {
        prizeService.assignPrize(userDetails, eventId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Gán giải thưởng thành công."));
    }

    @GetMapping("/{eventId}/prize")
    public ResponseEntity<ApiResponse<PrizeResponseDTO>> getPrize(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer eventId) {
        PrizeResponseDTO responseDTOS = prizeService.getPrize(userDetails, eventId);
        return ResponseEntity.ok(ApiResponse.success(responseDTOS, "Sinh viên  xem giải thưởng thành công."));
    }
    @GetMapping("/{eventId}/coordinator/prize")
    public ResponseEntity<ApiResponse<List<PrizeResponseDTO>>> getPrizeForCoordinator(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer eventId) {
        List<PrizeResponseDTO> responseDTOS = prizeService.getPrizeForCoordinator(userDetails, eventId);
        return ResponseEntity.ok(ApiResponse.success(responseDTOS, "Ban tổ chức xem giải thưởng thành công."));
    }


}
