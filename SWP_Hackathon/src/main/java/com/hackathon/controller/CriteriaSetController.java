package com.hackathon.controller;


import com.hackathon.dto.criteria.CreateCriteriaSetRequest;
import com.hackathon.dto.criteria.CriteriaDetailResponseDTO;
import com.hackathon.dto.criteria.CriteriaSetRequestDTO;
import com.hackathon.dto.criteria.CriteriaSetResponseDTO;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.CriteriaSetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/criteriaSet")

public class CriteriaSetController {
    @Autowired
    private CriteriaSetService criteriaSetService;

    //1. get all bo tieu chi hien co(criteria-set)
    @GetMapping
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<List<CriteriaSetResponseDTO>>>getAllCriteriaSets() {
        List<CriteriaSetResponseDTO> list =  criteriaSetService.getAllCriteriaSets();
        return ResponseEntity.ok(ApiResponse.success(list, "Nhận tất cả bộ tiêu chí thành công"));
    }
   //2. Lay all thong tin trong bo tiey chi chi tiet (criteria-detail)
    @GetMapping("criteria-detail")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<List<CriteriaDetailResponseDTO>>> getCriteriaSet() {
        List<CriteriaDetailResponseDTO> list = criteriaSetService.getAllCriteriaDetail();
        return ResponseEntity.ok(ApiResponse.success(list,"Nhận bộ tiêu chí chi tiết thành công"));
    }

    //3.  Lay tat ca thong tin trong bo tieu chi goc(template) va tieu chi chi tiet trong template
    @GetMapping("/with-details")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<List<CriteriaSetResponseDTO> >>getAllCriteriaSetDetail(){
     List<CriteriaSetResponseDTO>   list = criteriaSetService.getAllCriteriaSetDetail();
     return ResponseEntity.ok(ApiResponse.success(list,"Nhận tất cả thông tin bộ tiêu chí và tiêu chí chi tiết thành công"));

    }
    //4. Lay thong tin Criteria_Detail bang ID cua bo tieu chi (Set).
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<CriteriaSetResponseDTO>> getCriteriaDetailByCriteriaSet(@PathVariable ("id") Integer criteriaSetId ){
        CriteriaSetResponseDTO list = criteriaSetService.getCriteriaDetailById(criteriaSetId);
        return ResponseEntity.ok(ApiResponse.success(list,"Get all info of Criteria-Detail by CriteriaSet"));
    }
    //5. Tao CriteriaSet
    @PostMapping("/create-criteriaSet")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<CriteriaSetResponseDTO>> createCriteriaSet(@RequestBody CreateCriteriaSetRequest request, @AuthenticationPrincipal CustomUserDetails user){
        System.out.println("USER DETAILS = " + user);
            CriteriaSetResponseDTO create = criteriaSetService.createCriteriaSet(request, user);
            return ResponseEntity.ok(ApiResponse.success(create,"Tạo bộ tiêu chí thành công"));
    }
    // 6. Update CriteriaSet
    @PostMapping("/update-criteriaSet")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<CriteriaSetResponseDTO>> updateCriteriaSet(@RequestBody  CriteriaSetRequestDTO request, @AuthenticationPrincipal CustomUserDetails user){
        CriteriaSetResponseDTO update = criteriaSetService.updateCriteriaSet(request , user);
        return ResponseEntity.ok(ApiResponse.success(update,"Cập nhật bộ tiêu chí thành công"));
    }

    // 7. Xoa bo tieu chi
    @PostMapping("/delete-criteriaSet/{criteriaSetId}")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<Void>> deleteCriteriaSet(@PathVariable Integer criteriaSetId, @AuthenticationPrincipal CustomUserDetails user){
          criteriaSetService.deleteCriteriaSet(criteriaSetId, user);
        return ResponseEntity.ok(ApiResponse.success(null,"Xóa bộ tiêu chí thành công"));
    }

}
