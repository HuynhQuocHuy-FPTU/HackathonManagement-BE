package com.hackathon.controller;

import com.hackathon.dto.category.CategoryRoundDTO;
import com.hackathon.dto.categoryRound.CategoryRoundResponseDTO;
import com.hackathon.dto.event.EventDTO;
import com.hackathon.dto.round.RoundDTO;
import com.hackathon.dto.submission.SubmissionResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.repository.RoundRepository;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.CategoryRoundService;
import com.hackathon.service.ExpertAssignService;
import com.hackathon.service.impl.SubmissionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/expert/assigments")
@RequiredArgsConstructor
public class ExpertAssigmentController {
    private final ExpertAssignService expertAssignService;
    private final SubmissionServiceImpl submissionServiceImpl;
    private final RoundRepository roundRepository;
    private final CategoryRoundService categoryRoundService;

    //1. Lấy danh sách event được phân công
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<EventDTO>>> getEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<EventDTO> list = expertAssignService.getEventForJudge(userDetails);
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách cuộc thi thành công"));
    }
    //2. Lấy danh sách round thuộc event được phân công
    @GetMapping("/events/{eventId}/rounds")
    public ResponseEntity<ApiResponse<List<RoundDTO>>> getRounds(
            @PathVariable Integer eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RoundDTO> list = expertAssignService.getRoundForJudge(userDetails, eventId);
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách vòng thi thuộc cuộc thi thành công"));
    }
    //3. Lấy danh sách category thuộc event được phân công
    @GetMapping("/rounds/{roundId}/categories")
    public ResponseEntity<ApiResponse<List<CategoryRoundDTO>>> getCategories(
            @PathVariable Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CategoryRoundDTO> list = expertAssignService.getCategoryRoundForJudge(userDetails, roundId);
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách hạng mục thuộc vòng thi thành công"));
    }

    //4. Lấy danh sách submission thuộc categoryRound được phân công
    @GetMapping("/category-round/{categoryRoundId}/submissions")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissions(
            @PathVariable Integer categoryRoundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<SubmissionResponse> list = submissionServiceImpl.getSubmissionForJudge(userDetails, categoryRoundId);
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách bài nộp thuộc hạng mục của vòng đấu thành công"));
    }

    @GetMapping("/assigments/all-roles/{eventId}")
    public ResponseEntity<ApiResponse<List<CategoryRoundResponseDTO>>>getAllAssignedRoles(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer eventId){
        List<CategoryRoundResponseDTO> list = categoryRoundService.getAllAssignedCategoryRounds(userDetails, eventId);
        return ResponseEntity.ok(ApiResponse.success(list, "Xem tất cả quyền của expert thành công"));
    }

}
