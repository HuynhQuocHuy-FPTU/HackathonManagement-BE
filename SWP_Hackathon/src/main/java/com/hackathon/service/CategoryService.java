package com.hackathon.service;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.category.CreateCategoryRequest;
import com.hackathon.entity.Category;

public interface CategoryService {
    public Category createCategory(CreateCategoryRequest request, int eventId);
    public CategoryResponse mapToResponse(Category category);

}
