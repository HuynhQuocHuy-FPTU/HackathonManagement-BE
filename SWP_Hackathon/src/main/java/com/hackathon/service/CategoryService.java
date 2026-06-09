package com.hackathon.service;

import com.hackathon.dto.category.request.CreateCategoryRequest;
import com.hackathon.entity.Category;

public interface CategoryService {
    public Category createCategory(CreateCategoryRequest request);

}
