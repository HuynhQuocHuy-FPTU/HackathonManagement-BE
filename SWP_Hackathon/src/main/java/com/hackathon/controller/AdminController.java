package com.hackathon.controller;

import com.hackathon.dto.AuditLogResponse;
import com.hackathon.dto.auth.InviteAccountRequest;
import com.hackathon.service.AdminService;
import com.hackathon.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AuditService auditService;
    @GetMapping("/auditLog")
    public ResponseEntity<List<AuditLogResponse>> getAllAuditLog(){
        List<AuditLogResponse> list = auditService.getAllAuditLog();
        return ResponseEntity.ok(list);
    }
//    private final AdminService adminService;
//    @PostMapping("invite")
//    public ResponseEntity<String> inviteAccount(@Valid @RequestBody InviteAccountRequest request){
//        String result = adminService.inviteAccountByAdmin(request);
//        return ResponseEntity.ok(result);
//    }

}
