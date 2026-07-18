package com.hackathon.service;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.categoryRound.CategoryRoundResponseDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.ExpertRole;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.CategoryRoundRepository;
import com.hackathon.repository.ExpertAssignRepository;
import com.hackathon.repository.ExpertRepository;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryRoundServiceImpl implements CategoryRoundService {
    @Autowired
    private CategoryRoundRepository categoryRoundRepository;
    @Autowired
    private HackathonEventRepository hackathonEventRepository;
    @Autowired
    private ExpertAssignRepository expertAssignRepository;

    @Autowired
    private ExpertRepository expertRepository;

    @Override
    public List<CategoryRound> createCategoryRound(List<Category> categories, Round round) {
        List<CategoryRound> categoryRounds = new ArrayList<>();

        for (Category category : categories) {
            CategoryRound categoryRound = new CategoryRound();
            categoryRound.setRound(round);
            categoryRound.setCategory(category);
            categoryRounds.add(categoryRound);

            // Đồng bộ chiều ngược ở CẢ HAI phía cha, bắt buộc vì orphanRemoval=true ở cả hai
            category.getCategoryRounds().add(categoryRound);
            round.getCategoryRounds().add(categoryRound);
        }

        categoryRounds = categoryRoundRepository.saveAll(categoryRounds);
        categoryRoundRepository.flush();
        return categoryRounds;
    }

    @Override
    public void deleteByEventId(Integer eventId) {
        categoryRoundRepository.deleteByEventId(eventId);
    }

    @Override
    public List<CategoryRoundResponseDTO> getAllCategory(Integer eventId) {
        HackathonEvent hackathonEvent = hackathonEventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy event"));
        List<CategoryRound> categoryRounds = categoryRoundRepository.findByRound_HackathonEvent_EventId(eventId);
        if (categoryRounds.isEmpty()) {
            throw new BadRequestException("Không có hạng mục nào trong cuộc thi này.");
        }
        List<CategoryRoundResponseDTO> dtoList = new ArrayList<>();
        for (CategoryRound cr : categoryRounds) {
            CategoryRoundResponseDTO dto = CategoryRoundResponseDTO.builder()
                    .roundId(cr.getRound().getRoundId())
                    .roundName(cr.getRound().getRoundName())
                    .categoryRoundId(cr.getCategoryRoundId())
                    .categoryId(cr.getCategory().getCategoryId())
                    .categoryName(cr.getCategory().getCategoryName()).build();
            dtoList.add(dto);
        }
        return dtoList;
    }

    //Mentor xem tất cả các CategoryRound mình được phân công trong trạng thái EVENT ĐANG DIỄN RA
    @Override
    public List<CategoryRoundResponseDTO> getAssignedCategoryRounds(CustomUserDetails userDetails, Integer eventId) {
        Expert expert = expertRepository.findByAccount_AccountId(userDetails.getAccount().getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Expert"));
//        List<ExpertAssign> mentorAssignments =
//                expert.getExpertAssigns().stream()
//                        .filter(a -> a.getRole() == ExpertRole.MENTOR)
//                        .toList();
        List<ExpertAssign> mentorAssignments =
                expertAssignRepository.findExpertAssignmentsByRole(
                        expert.getExpertId(),
                        ExpertRole.MENTOR,
                        eventId
                );
        List<CategoryRoundResponseDTO> dtoList = new ArrayList<>();
        for (ExpertAssign ex : mentorAssignments) {
            CategoryRound cr = ex.getCategoryRound();

            CategoryRoundResponseDTO dto = CategoryRoundResponseDTO.builder()
                    .roundId(cr.getRound().getRoundId())
                    .roundName(cr.getRound().getRoundName())
                    .roundDate(cr.getRound().getStartTime())
                    .roundEnd(cr.getRound().getEndTime())
                    .categoryRoundId(cr.getCategoryRoundId())
                    .categoryId(cr.getCategory().getCategoryId())
                    .categoryName(cr.getCategory().getCategoryName())
                    .role(ex.getRole()).build();
            dtoList.add(dto);
        }

        return dtoList;
    }

    @Override
    public List<CategoryRoundResponseDTO> getAllAssignedCategoryRounds(CustomUserDetails userDetails, Integer eventId) {
        // 1. Tìm thông tin của Expert dựa vào tài khoản đang đăng nhập
        Expert expert = expertRepository.findByAccount_AccountId(userDetails.getAccount().getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Expert"));

        // 2. Lấy TẤT CẢ các phân công (assignments) của Expert này trong sự kiện (event)
        // Điểm khác biệt mấu chốt là dùng findExpertAssignments để lấy hết mọi Role,
        // chứ không dùng findExpertAssignmentsByRole(... , ExpertRole.MENTOR, ...) như hàm cũ!
        List<ExpertAssign> allAssignments = expertAssignRepository.findExpertAssignments(expert.getExpertId(), eventId);

        // 3. Mapping dữ liệu trả về cho Frontend
        List<CategoryRoundResponseDTO> dtoList = new ArrayList<>();
        for (ExpertAssign ex : allAssignments) {
            CategoryRound cr = ex.getCategoryRound();
            CategoryRoundResponseDTO dto = CategoryRoundResponseDTO.builder()
                    .roundId(cr.getRound().getRoundId())
                    .roundName(cr.getRound().getRoundName())
                    .roundDate(cr.getRound().getStartTime())
                    .roundEnd(cr.getRound().getEndTime())
                    .categoryRoundId(cr.getCategoryRoundId())
                    .categoryId(cr.getCategory().getCategoryId())
                    .categoryName(cr.getCategory().getCategoryName())
                    .role(ex.getRole()) // Trả về cả role hiện tại (MENTOR/CORE_JUDGE/GUEST_JUDGE) để FE phân loại
                    .build();
            dtoList.add(dto);
        }

        return dtoList;
    }

}
