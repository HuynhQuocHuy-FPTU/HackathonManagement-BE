package com.hackathon.controller;
import com.hackathon.dto.participant.RoundParticipantDetailDTO;
import com.hackathon.dto.team.CategoryAdvancementResultDTO;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.ParticipantService;
import com.hackathon.service.impl.RoundAdvancementServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/round")
@RequiredArgsConstructor
public class RoundAdvancementController {
    private final RoundAdvancementServiceImpl roundAdvancementServiceImpl;
    private final ParticipantService participantService;

    @GetMapping("/{roundId}/participant/detail")
    public ResponseEntity<ApiResponse<RoundParticipantDetailDTO>> getParticipantDetailByRound(@PathVariable Integer roundId, @AuthenticationPrincipal CustomUserDetails userDetails){

        return ResponseEntity.ok(ApiResponse.success(participantService.getDetailParticipantByRound(roundId, userDetails), "Thông tin chi tiết của từng team trong round chia theo category"));
    }

    @PostMapping("/advancement/{roundId}")
    public ResponseEntity<ApiResponse<List<CategoryAdvancementResultDTO>>> advanceRound(@PathVariable Integer roundId, @AuthenticationPrincipal CustomUserDetails userDetails){
        List<CategoryAdvancementResultDTO> list = roundAdvancementServiceImpl.advanceAllCategoriesInRound(roundId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(list, "Thăng vòng thành công"));
    }




}
