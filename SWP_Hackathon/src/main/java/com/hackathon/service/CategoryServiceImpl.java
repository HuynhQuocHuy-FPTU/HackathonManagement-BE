package com.hackathon.service;

import com.hackathon.dto.category.CreateCategoryRequest;
import com.hackathon.entity.Category;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.repository.CategoryRepository;
import com.hackathon.repository.HackathonEventRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Data
public class CategoryServiceImpl implements CategoryService{

    private final CategoryRepository categoryRepository;

    private final HackathonEventRepository eventRepository;

    @Override
    public Category createCategory(CreateCategoryRequest request) {
        // 1. Get event id
        HackathonEvent event = eventRepository.findById(request.getEventId()).orElseThrow(() -> new RuntimeException("Event not found"));

        //2. create category
        Category category = new Category();
        category.setCategoryName(request.getCategoryName());
        category.setHackathonEvent(event);

        //3. save DB
        return categoryRepository.save(category);
    }
}
