package com.hackathon.service;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.category.CreateCategoryRequest;
import com.hackathon.dto.category.UpdateCategoryRequest;
import com.hackathon.entity.Category;
import com.hackathon.entity.HackathonEvent;

import java.util.List;

public interface CategoryService {
    public List<Category> createCategory(List<CreateCategoryRequest> request, int eventId);
    public CategoryResponse mapToResponse(Category category);
    public List<CategoryResponse> getAllCategories(Integer eventId);
    public List<Category> updateCategories(List<UpdateCategoryRequest> categories, HackathonEvent event);
}
