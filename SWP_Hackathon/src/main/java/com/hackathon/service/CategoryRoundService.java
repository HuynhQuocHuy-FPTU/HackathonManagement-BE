package com.hackathon.service;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.categoryRound.CategoryRoundResponseDTO;
import com.hackathon.entity.Category;
import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Round;
import com.hackathon.security.CustomUserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface CategoryRoundService {
     List<CategoryRound> createCategoryRound(List<Category> categories, Round round);

    void deleteByEventId(Integer eventId);

    List<CategoryRoundResponseDTO> getAssignedCategoryRounds(CustomUserDetails userDetails, Integer eventId);

    List<CategoryRoundResponseDTO> getAllAssignedCategoryRounds(CustomUserDetails userDetails, Integer eventId);
}
