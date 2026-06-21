package com.hackathon.controller;

import com.hackathon.dto.auth.InviteAccountRequest;
import com.hackathon.dto.team.TeamDetailResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.AdminService;
import com.hackathon.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final TeamService teamService;

//    private final AdminService adminService;
//    @PostMapping("invite")
//    public ResponseEntity<String> inviteAccount(@Valid @RequestBody InviteAccountRequest request){
//        String result = adminService.inviteAccountByAdmin(request);
//        return ResponseEntity.ok(result);
//    }
    //ADMIN Lấy list Team để quản lý
    @GetMapping("/list-teams")
    public ResponseEntity<ApiResponse<List<TeamDetailResponse>>> getTeamForAdmin(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TeamDetailResponse> list = teamService.getTeamForAdmin(userDetails);
        return ResponseEntity.ok(ApiResponse.success(list,"Lấy danh sách team thành công"));
    }
}
