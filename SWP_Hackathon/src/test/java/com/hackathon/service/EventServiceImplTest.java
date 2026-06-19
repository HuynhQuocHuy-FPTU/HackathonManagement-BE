//package com.hackathon.service;
//
//import com.hackathon.dto.category.CreateCategoryRequest; // 🔥 Đã cập nhật đúng DTO của bạn
//import com.hackathon.dto.event.CreateEventRequest;
//import com.hackathon.dto.round.CreateRoundRequest;
//import com.hackathon.entity.*;
//import com.hackathon.exception.BadRequestException;
//import com.hackathon.repository.CategoryRoundRepository;
//import com.hackathon.repository.EventCoordinatorRepository;
//import com.hackathon.repository.HackathonEventRepository;
//import com.hackathon.validator.EventValidator;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class EventServiceImplTest {
//
//    @Mock
//    private HackathonEventRepository eventRepository;
//    @Mock
//    private EventCoordinatorRepository eventCoordinatorRepository;
//    @Mock
//    private EventValidator eventValidator;
//    @Mock
//    private CategoryService categoryService;
//    @Mock
//    private RoundService roundService;
//    @Mock
//    private CategoryRoundRepository categoryRoundRepository;
//
//    @InjectMocks
//    private EventServiceImpl eventService;
//
//    private CreateEventRequest request;
//    private EventCoordinator coordinator;
//    private HackathonEvent savedEvent;
//    private Category savedCategory;
//    private Round savedRound;
//
//    @BeforeEach
//    void setUp() {
//        // 1. Khởi tạo cấu trúc Request tổng dựa trên các DTO thực tế của bạn
//        request = new CreateEventRequest();
//        request.setEventName("RMIT Hackathon 2026");
//        request.setStartDate(LocalDateTime.of(2026, 6, 1, 9, 0)); // Tháng 6 -> Mùa hè (SUMMER)
//        request.setEndDate(LocalDateTime.of(2026, 6, 3, 17, 0));
//
//        // Setup Categories sử dụng chính xác CreateCategoryRequest
//        List<CreateCategoryRequest> categoryRequests = new ArrayList<>();
//        CreateCategoryRequest catReq = new CreateCategoryRequest();
//        catReq.setCategoryName("Bảng Sinh Viên");
//        categoryRequests.add(catReq);
//        request.setCategories(categoryRequests);
//
//        // Setup Rounds sử dụng CreateRoundRequest
//        List<CreateRoundRequest> roundRequests = new ArrayList<>();
//        CreateRoundRequest roundReq = new CreateRoundRequest();
//        roundReq.setRoundName("Vòng Sơ Loại");
//        roundReq.setCriteriaSetId(1);
//        roundReq.setAppliedListCategoryNames(List.of("Bảng Sinh Viên"));
//
//        // Mock thêm data tiêu chí con nằm trong Round
//        CustomCriteriaRound criteriaRoundReq = new CustomCriteriaRound();
//        criteriaRoundReq.setCriteriaDetailId(101);
//        criteriaRoundReq.setCustomWeight(100.0);
//        roundReq.setCustomCriteriaRounds(List.of(criteriaRoundReq));
//
//        roundRequests.add(roundReq);
//        request.setRounds(roundRequests);
//
//        // 2. Khởi tạo các thực thể Entity giả lập làm kết quả trả về từ DB/Service
//        coordinator = new EventCoordinator();
//        coordinator.setCoordinatorId(1);
//
//        savedEvent = new HackathonEvent();
//        savedEvent.setEventId(100);
//        savedEvent.setEventName(request.getEventName());
//
//        savedCategory = new Category();
//        savedCategory.setCategoryId(200);
//        savedCategory.setCategoryName("Bảng Sinh Viên");
//
//        savedRound = new Round();
//        savedRound.setRoundId(300);
//        savedRound.setRoundName("Vòng Sơ Loại");
//    }
//
//    @Test
//    @DisplayName("TC-01: Tạo Event thành công với đầy đủ Category, Round và đóng băng tiêu chí")
//    void createEvent_Success() throws BadRequestException {
//        // Given (Định nghĩa hành vi cho các mock object)
//        doNothing().when(eventValidator).validatorCreate(request);
//        when(eventCoordinatorRepository.findById(1)).thenReturn(Optional.of(coordinator));
//        when(eventRepository.save(any(HackathonEvent.class))).thenReturn(savedEvent);
//        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(savedCategory);
//        when(roundService.createRound(any(CreateRoundRequest.class))).thenReturn(savedRound);
//        when(categoryRoundRepository.save(any(CategoryRound.class))).thenReturn(new CategoryRound());
//
//        // When (Thực thi hàm nghiệp vụ chính)
//        assertDoesNotThrow(() -> eventService.createEvent(request));
//
//        // Then (Xác minh số lần tương tác và tính đúng đắn)
//        verify(eventValidator, times(1)).validatorCreate(request);
//        verify(eventRepository, times(1)).save(any(HackathonEvent.class));
//        verify(categoryService, times(1)).createCategory(any(CreateCategoryRequest.class));
//        verify(roundService, times(1)).createRound(any(CreateRoundRequest.class));
//        verify(categoryRoundRepository, times(1)).save(any(CategoryRound.class));
//    }
//
//    @Test
//    @DisplayName("TC-02: Hệ thống ném lỗi BadRequestException nếu không tìm thấy Coordinator")
//    void createEvent_Failed_CoordinatorNotFound() throws BadRequestException {
//        // Given
//        doNothing().when(eventValidator).validatorCreate(request);
//        when(eventCoordinatorRepository.findById(1)).thenReturn(Optional.empty()); // Trả về rỗng để kích hoạt orElseThrow
//
//        // When & Then
//        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
//            eventService.createEvent(request);
//        });
//
//        assertEquals("Coodinator not found", exception.getMessage());
//
//        // Đảm bảo hệ thống dừng ngay lập tức, không lưu bừa bãi xuống Database
//        verify(eventRepository, never()).save(any(HackathonEvent.class));
//        verify(categoryService, never()).createCategory(any(CreateCategoryRequest.class));
//    }
//
//    @Test
//    @DisplayName("TC-03: Phải dừng lưu bảng trung gian Category_Round nếu quá trình copy tiêu chí ở Round bị lỗi")
//    void createEvent_Failed_WhenRoundServiceThrowsException() throws BadRequestException {
//        // Given
//        doNothing().when(eventValidator).validatorCreate(request);
//        when(eventCoordinatorRepository.findById(1)).thenReturn(Optional.of(coordinator));
//        when(eventRepository.save(any(HackathonEvent.class))).thenReturn(savedEvent);
//        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(savedCategory);
//
//        // Giả lập kịch bản lỗi ném ra từ tầng RoundService (Ví dụ: Bất nhất tiêu chí)
//        when(roundService.createRound(any(CreateRoundRequest.class)))
//                .thenThrow(new RuntimeException("Phát hiện lỗi bất nhất! Tiêu chí con không nằm trong Bộ tiêu chí tổng đã chọn."));
//
//
//        // When & Then
//        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
//            eventService.createEvent(request);
//        });
//
//        assertEquals("Phát hiện lỗi bất nhất! Tiêu chí con không nằm trong Bộ tiêu chí tổng đã chọn.", exception.getMessage());
//
//        // Đảm bảo tuyệt đối bảng trung gian CategoryRound chưa hề được gọi lưu (Transaction sẽ rollback sạch rác)
//        verify(categoryRoundRepository, never()).save(any(CategoryRound.class));
//    }
//}