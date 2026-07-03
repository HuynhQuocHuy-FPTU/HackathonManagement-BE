package com.hackathon.service;

import com.hackathon.entity.Category;
import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Round;
import com.hackathon.repository.CategoryRoundRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class CategoryRoundServiceImpl implements CategoryRoundService{
    @Autowired
    private CategoryRoundRepository categoryRoundRepository;

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


}
