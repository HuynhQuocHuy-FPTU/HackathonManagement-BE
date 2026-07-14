package com.hackathon.controller;

import com.hackathon.dto.event.PrizeRequestDTO;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.prize.PrizeServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/prize")
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

}
