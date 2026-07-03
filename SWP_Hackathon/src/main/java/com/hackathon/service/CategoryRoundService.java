package com.hackathon.service;

import com.hackathon.entity.Category;
import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Round;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface CategoryRoundService {
    public List<CategoryRound> createCategoryRound(List<Category> categories, Round round);

    void deleteByEventId(Integer eventId);

}
