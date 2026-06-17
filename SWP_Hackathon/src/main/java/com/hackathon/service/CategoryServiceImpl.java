package com.hackathon.service;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.category.CreateCategoryRequest;
import com.hackathon.dto.category.UpdateCategoryRequest;
import com.hackathon.entity.Category;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.CategoryRepository;
import com.hackathon.repository.HackathonEventRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private HackathonEventRepository eventRepository;

    @Override
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
    public List<CategoryResponse> getAllCategories() {
        return List.of();
    }


//    @Override
//    @Transactional
//    public List<Category> updateCategories(List<UpdateCategoryRequest> categoryRequests, HackathonEvent event) {
//        // 1. Kiểm tra nếu request rỗng, xóa sạch category của event này
//        if (categoryRequests == null || categoryRequests.isEmpty()) {
//            if (event.getCategories() != null) {
//                event.getCategories().clear();
//            }
//            return new ArrayList<>();
//        }
//
//        // Lấy danh sách category hiện tại đang được quản lý bởi Hibernate từ chính thực thể Event
//        List<Category> currentCategories = event.getCategories();
//        if (currentCategories == null) {
//            currentCategories = new ArrayList<>();
//            event.setCategories(currentCategories);
//        }
//
//        // 2. XÓA: Lọc các ID có trong request gửi lên
//        List<Integer> updateCategoryIds = categoryRequests.stream()
//                .map(UpdateCategoryRequest::getCategoryId)
//                .filter(id -> id != null)
//                .collect(Collectors.toList());
//
//        // Sử dụng removeIf trực tiếp trên danh sách của Hibernate quản lý.
//        // Các category bị loại bỏ tại đây sẽ tự động bị xóa khỏi DB nhờ 'orphanRemoval = true'
//        currentCategories.removeIf(current -> !updateCategoryIds.contains(current.getCategoryId()));
//
//        // 3. CẬP NHẬT HOẶC TẠO MỚI
//        for (UpdateCategoryRequest dto : categoryRequests) {
//            if (dto.getCategoryId() != null) {
//                // --- TRƯỜNG HỢP 1: CÓ ID -> CẬP NHẬT (UPDATE) ---
//                // Tìm phần tử cũ trực tiếp trong list hiện tại để cập nhật data (Không tạo mới, giữ nguyên ID cũ)
//                currentCategories.stream()
//                        .filter(c -> c.getCategoryId().equals(dto.getCategoryId()))
//                        .findFirst()
//                        .ifPresentOrElse(
//                                existingCategory -> {
//                                    // Chỉ cập nhật các trường thay đổi
//                                    existingCategory.setCategoryName(dto.getCategoryName());
//                                },
//                                () -> {
//                                    throw new BadRequestException("Không tìm thấy Category với ID: " + dto.getCategoryId());
//                                }
//                        );
//            } else {
//                // --- TRƯỜNG HỢP 2: KHÔNG CÓ ID -> TẠO MỚI (CREATE) ---
//                Category newCategory = new Category();
//                newCategory.setCategoryName(dto.getCategoryName());
//                newCategory.setHackathonEvent(event); // Gắn quan hệ cha-con
//
//                // Add trực tiếp vào danh sách của Hibernate, DB sẽ chỉ sinh duy nhất 1 lệnh INSERT cho bản ghi mới này
//                currentCategories.add(newCategory);
//            }
//        }
//
//        // 4. KHÔNG dùng categoryRepository.saveAll() nữa.
//        // Chỉ cần trả về danh sách, vì có @Transactional, Hibernate tự động đồng bộ mọi thay đổi xuống DB khi hết hàm.
//        return currentCategories;
//    }

    @Transactional
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
