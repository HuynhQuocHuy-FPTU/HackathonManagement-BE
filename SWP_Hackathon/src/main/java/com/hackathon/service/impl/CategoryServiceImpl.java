package com.hackathon.service.impl;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.category.CreateCategoryRequest;
import com.hackathon.dto.category.UpdateCategoryRequest;
import com.hackathon.entity.Category;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.CategoryRepository;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
// Quản lý các danh mục thi đấu được cấu hình cho từng sự kiện.
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private HackathonEventRepository eventRepository;

    @Override
    // Tạo nhiều danh mục cho sự kiện và kiểm tra tránh dữ liệu danh mục không hợp lệ.
    public List<Category> createCategory(List<CreateCategoryRequest> requests, int eventId) {
        // 1. Get event id
        HackathonEvent event = eventRepository.findById(eventId).orElseThrow(() -> new BadRequestException("Event not found"));

        //2. create category
        List<Category> categories = new ArrayList<>();
        for (CreateCategoryRequest request : requests) {
            Category category = new Category();
            category.setCategoryName(request.getCategoryName());
            category.setHackathonEvent(event);
            categories.add(category);
        }

        //3. save DB
        categories = categoryRepository.saveAll(categories);
        return categories;

    }

    @Override
    public CategoryResponse mapToResponse(Category category) {

        return CategoryResponse.builder().categoryId(category.getCategoryId()).categoryName(category.getCategoryName()).build();
    }

    @Override
    // Lấy toàn bộ danh mục thuộc sự kiện được yêu cầu.
    public List<CategoryResponse> getAllCategories(Integer eventId) {
        List<Category> categories = categoryRepository.findAllByHackathonEvent_EventId(eventId);

        List<CategoryResponse> responses = categories.stream().map(cate -> this.mapToResponse(cate)).toList();
        return responses;
    }

    @Transactional
    // Đồng bộ danh sách danh mục mới với các danh mục hiện có của sự kiện.
    public List<Category> updateCategories(List<UpdateCategoryRequest> categoryRequests, HackathonEvent event) {
        if (categoryRequests == null) return new ArrayList<>();

        // 1. Lấy danh sách hiện tại
        List<Category> currentCategories = event.getCategories();
        if (currentCategories == null) currentCategories = new ArrayList<>();

        // 2. Map các ID có trong request để biết cái nào cần giữ lại
        List<Integer> updateCategoryIds = categoryRequests.stream()
                .map(UpdateCategoryRequest::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 3. Xóa các cái không có trong request (nhưng vẫn giữ lại cái cũ nếu muốn)
        currentCategories.removeIf(c -> !updateCategoryIds.contains(c.getCategoryId()));

        // 4. Update hoặc Add mới
        for (UpdateCategoryRequest dto : categoryRequests) {
            if (dto.getCategoryId() != null) {
                // Cập nhật cái cũ
                currentCategories.stream()
                        .filter(c -> c.getCategoryId().equals(dto.getCategoryId()))
                        .findFirst()
                        .ifPresent(c -> c.setCategoryName(dto.getCategoryName()));
            } else {
                // Thêm mới
                Category newCat = new Category();
                newCat.setCategoryName(dto.getCategoryName());
                newCat.setHackathonEvent(event);
                currentCategories.add(newCat);
            }
        }

        // Đảm bảo event liên kết với danh sách mới nhất
        event.setCategories(currentCategories);
        return currentCategories;
    }
}
