package com.hackathon.service.impl;

import com.hackathon.dto.criteria.*;
import com.hackathon.dto.history.CriteriaHistoryResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AuditAction;
import com.hackathon.entity.enums.AuditEntityType;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.CriteriaSetService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;


@Service
@RequiredArgsConstructor
// Quản lý bộ tiêu chí mẫu, các tiêu chí thành phần và lịch sử thay đổi của từng bộ.
public class CriteriaSetServiceImpl implements CriteriaSetService {

    private final CriteriaSetRepository criteriaSetRepository;
    private final CriteriaDetailRepository criteriaDetailRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final AuditService auditService;
    private final AccountRepository accountRepository;
    private final AuditLogRepository auditLogRepository;
    private final HackathonEventRepository hackathonEventRepository;

    // 1. Get all bo tieu chi hien co(criteria-set)
    @Override
    // Lấy danh sách rút gọn của tất cả bộ tiêu chí để hiển thị và lựa chọn.
    public List<CriteriaSetResponseDTO> getAllCriteriaSets() {
        List<CriteriaSet> criteriaSets = criteriaSetRepository.findAll();
        return criteriaSets.stream()
                .map(criteriaSet -> {
                    CriteriaSetResponseDTO dto = new CriteriaSetResponseDTO();
                    dto.setCriteriaSetId(criteriaSet.getCriteriaSetId());
                    dto.setCriteriaSetName(criteriaSet.getCriteriaSetName());
                    dto.setMaxScore(criteriaSet.getMaxScore());
                    return dto;
                }).toList();
    }

    // 3.  Lay tat ca thong tin trong bo tieu chi goc(template) va tieu chi chi tiet trong template
    @Override
    @Transactional
    // Lấy đầy đủ từng bộ tiêu chí cùng các tiêu chí thành phần đang thuộc bộ đó.
    public List<CriteriaSetResponseDTO> getAllCriteriaSetDetail() {

        List<CriteriaSet> sets = criteriaSetRepository.findAll();

        return sets.stream().map(set -> {

            CriteriaSetResponseDTO dto = new CriteriaSetResponseDTO();
            dto.setCriteriaSetId(set.getCriteriaSetId());
            dto.setCriteriaSetName(set.getCriteriaSetName());
            dto.setMaxScore(set.getMaxScore());
            List<CriteriaDetailResponseDTO> details = set.getCriteriaDetails()
                    .stream()
                    .map(d -> new CriteriaDetailResponseDTO(
                            d.getCriteriaId(),
                            d.getCriteriaName(),
                            d.getWeight(),
                            d.getCriteriaType(),
                            d.getDescription()
                    ))
                    .toList();

            dto.setCriteriaDetails(details);

            return dto;
        }).toList();
    }

    //2. Lay tat ca thong tin trong tieu chi chi tiet(detail) hien thi
    @Override
    // Lấy toàn bộ tiêu chí chi tiết hiện có trong hệ thống.
    public List<CriteriaDetailResponseDTO> getAllCriteriaDetail() {

        return criteriaDetailRepository.findAll()
                .stream()
                .map(cri -> new CriteriaDetailResponseDTO(
                        cri.getCriteriaId(),
                        cri.getCriteriaName(),
                        cri.getWeight(),
                        cri.getCriteriaType(),
                        cri.getDescription()
                ))
                .toList();
    }

    //4.Thong qua ID Cua criteriaSet lay duoc ds criteriaDetail tuong ung vs id cua Set
    @Override
    // Tìm một bộ tiêu chí theo mã và trả về đầy đủ các tiêu chí thành phần.
    public CriteriaSetResponseDTO getCriteriaDetailById(Integer criteriaSetId) {
        // 1.
        CriteriaSet criteriaSet = criteriaSetRepository.findByCriteriaSetId(criteriaSetId);
        if (criteriaSet == null) {
            throw new BadRequestException("Không tìm thấy bộ tiêu chí: " + criteriaSetId);
        }
        //2. Lấy ds criteria-detail
        List<CriteriaDetail> details = criteriaDetailRepository.findByCriteriaSet_CriteriaSetId(criteriaSetId);
        if (details.isEmpty()) {
            throw new BadRequestException(
                    "Không tìm thấy bất kì tiêu chí nào trong bộ tiêu chí : " + criteriaSetId
            );
        }
        return mapToResponse(criteriaSet, details);

    }

    //5. Tao CriteriaSet
    @Override
    // Tạo bộ tiêu chí mới, lưu các tiêu chí thành phần và ghi nhận người thực hiện.
    public CriteriaSetResponseDTO createCriteriaSet(CreateCriteriaSetRequest request, CustomUserDetails userDetails) {
        // Check Coordinator mới là người được tạo
        Account account = userDetails.getAccount();
        EventCoordinator coordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không có quyền truy cập vào bộ tiêu chí"));

        // CriteriaSetName không được trùng
        String name = request.getCriteriaSetName().trim();
        if (criteriaSetRepository.existsByCriteriaSetName(name)) {
            throw new BadRequestException("Tên bộ tiêu chí không được phép trùng.");
        }

        // 1. Tao CriteriaSet
        CriteriaSet criteriaSet = new CriteriaSet();
        criteriaSet.setCriteriaSetName(name);
        criteriaSet.setMaxScore(request.getMaxScore());
        criteriaSet.setEventCoordinator(coordinator);

        // 2.Tao 1 list de luu Criteria-detail
        List<CriteriaDetail> list = new ArrayList<>();
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal hundred = new BigDecimal("100");
        for (CriteriaDetailRequestDTO dto : request.getCriteriaDetails()) {
            CriteriaDetail detail = new CriteriaDetail();
            detail.setCriteriaName(dto.getCriteriaName());
            detail.setWeight(dto.getWeight());
            detail.setDescription(dto.getDescription());
            detail.setCriteriaType(dto.getType());
            detail.setCriteriaSet(criteriaSet);
            detail.setCriteriaType(dto.getType());
            list.add(detail);
            totalWeight = totalWeight.add(dto.getWeight());
        }

        if (totalWeight.compareTo(hundred) != 0) {
            throw new BadRequestException("Tổng trọng số phải bằng 100");
        }
        criteriaSet.setCriteriaDetails(list);
        // 3. Luu du lieu xuong DB
        CriteriaSet saved = criteriaSetRepository.save(criteriaSet);
        List<CriteriaDetail> savedDetails = saved.getCriteriaDetails();
        auditService.saveLog(
                account,
                AuditAction.CREATE_CRITERIA,
                AuditEntityType.CRITERIA,
                criteriaSet.getCriteriaSetId(),
                "Create criteria " + criteriaSet.getCriteriaSetName()

        );
        return mapToResponse(saved, savedDetails);

    }

    // 6. Update CriteriaSet(Có thể thêm xóa , sữa các tiêu chí chi tiết , nhưng không được xóa tiêu chí cha)
    @Override
    @Transactional
    // Đồng bộ thông tin và danh sách tiêu chí của bộ hiện có, đồng thời lưu lịch sử thay đổi.
    public CriteriaSetResponseDTO updateCriteriaSet(CriteriaSetRequestDTO request, CustomUserDetails userDetails) {
        // Check Coordinator mới là người được tạo
        Account eventCoordinator = userDetails.getAccount();
        EventCoordinator coordinator =
                eventCoordinatorRepository
                        .findByAccount_AccountId(eventCoordinator.getAccountId())
                        .orElseThrow(() -> new BadRequestException(
                                "Bạn không có quyền truy cập vào bộ tiêu chí để thực hiện thao tác cập nhật dữ liệu bộ tiêu chí"
                        ));
        // Kiểm tra tên bộ tiêu chí không được null hoặc rỗng
        if (request.getCriteriaSetName() == null || request.getCriteriaSetName().trim().isEmpty()) {
            throw new BadRequestException("Tên bộ tiêu chí không được để trống.");
        }
        // CriteriaSetName không được trùng
        String name = request.getCriteriaSetName().trim();
        boolean isNameExist = criteriaSetRepository.existsByCriteriaSetNameAndCriteriaSetIdNot(name, request.getCriteriaSetId());
        if (isNameExist) {
            throw new BadRequestException("Tên bộ tiêu chí đã tồn tại trong hệ thống.");
        }

        //1. Lay bo tieu chi can update
        CriteriaSet criteriaSet = criteriaSetRepository
                .findByCriteriaSetId(request.getCriteriaSetId());
        if (criteriaSet == null) {
            throw new RuntimeException("CriteriaSet not found with id: " + request.getCriteriaSetId());
        }
        //2.Update info of criteria set
        boolean isCriteriaSetChanged = false;
        if ((criteriaSet.getCriteriaSetName() == null && request.getCriteriaSetName() != null) ||
                (criteriaSet.getCriteriaSetName() != null && !criteriaSet.getCriteriaSetName().equals(request.getCriteriaSetName()))) {
            criteriaSet.setCriteriaSetName(request.getCriteriaSetName());
            isCriteriaSetChanged = true;
        }

        if ((criteriaSet.getMaxScore() == null && request.getMaxScore() != null) ||
                (criteriaSet.getMaxScore() != null && !criteriaSet.getMaxScore().equals(request.getMaxScore()))) {
            criteriaSet.setMaxScore(request.getMaxScore());
            isCriteriaSetChanged = true;
        }
        // Nếu có đổi thì mới save, không thì giữ nguyên
        CriteriaSet savedCriteriaSet = isCriteriaSetChanged ? criteriaSetRepository.save(criteriaSet) : criteriaSet;
        //2.1 Lay ds criteria-detail thong qua ID cua Set(DB)
        List<CriteriaDetail> listDetail = criteriaDetailRepository.findByCriteriaSet_CriteriaSetId(request.getCriteriaSetId());

        // 3. Delete các tiêu chí detail
        // 3.2 Dùng Set để lưu các tiêu chí Detail thong qua id
        // Lấy ID từ request để check tiêu chí nào đã bị xóa
        Set<Integer> set = new HashSet<>();
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal hundred = new BigDecimal("100");
        for (CriteriaDetailRequestDTO dto : request.getCriteriaDetails()) {
            if (dto.getCriteriaId() != null) {
                set.add(dto.getCriteriaId());
            }
            if (dto.getCriteriaName() == null || dto.getCriteriaName().trim().isEmpty()) {
                throw new BadRequestException("Tên tiêu chí không được để trống!");
            }
            if (dto.getWeight() == null) {
                throw new BadRequestException("Trọng số (Weight) của tiêu chí không được để trống!");
            }
            if (dto.getType() == null) {
                throw new BadRequestException("Loại tiêu chí không được để trống!");
            }
            totalWeight = totalWeight.add(dto.getWeight());
        }
        if (totalWeight.compareTo(hundred) != 0) {
            throw new BadRequestException("Tổng các trọng số thành phần sau khi sửa đổi phải bằng 100. Hiện tại là: " + totalWeight);
        }
        // xóa những tiêu chí mà ng dùng xóa bỏ khỏi ds
        List<CriteriaDetail> deleteList = new ArrayList<>();
        for (CriteriaDetail criDetail : listDetail) {
            if (!set.contains(criDetail.getCriteriaId())) {
                deleteList.add(criDetail);
            }
        }
        if (!deleteList.isEmpty()) {
            criteriaDetailRepository.deleteAll(deleteList);
        }
        //2.2 Update info Of Criteria-detail(cũ or mới thêm )

        List<CriteriaDetail> updateList = new ArrayList<>();
        for (CriteriaDetailRequestDTO dto : request.getCriteriaDetails()) {
            // Check tiêu chí đó có hay chưa để thêm mới or update
            CriteriaDetail detail = null;
            if (dto.getCriteriaId() != null) {

                for (CriteriaDetail d : listDetail) {
                    if (d.getCriteriaId() == (dto.getCriteriaId())) {
                        detail = d;
                        break;
                    }
                }

                if (detail == null) {
                    throw new BadRequestException("Không tìm thấy tiêu chí nào với ID: " + dto.getCriteriaId());
                }

                // Thực hiện check thay đổi thủ công
                boolean isNameChanged = (detail.getCriteriaName() == null && dto.getCriteriaName() != null) ||
                        (detail.getCriteriaName() != null && !detail.getCriteriaName().equals(dto.getCriteriaName()));

                boolean isWeightChanged = (detail.getWeight() == null && dto.getWeight() != null) ||
                        (detail.getWeight() != null && !detail.getWeight().equals(dto.getWeight()));

                boolean isDescChanged = (detail.getDescription() == null && dto.getDescription() != null) ||
                        (detail.getDescription() != null && !detail.getDescription().equals(dto.getDescription()));

                boolean isTypeChanged = (detail.getCriteriaType() == null && dto.getType() != null) ||
                        (detail.getCriteriaType() != null && !detail.getCriteriaType().equals(dto.getType()));

                // Chỉ thực hiện thay đổi khi người dùng thực sự có sửa đổi nội dung ô nhập liệu
                if (isNameChanged || isWeightChanged || isDescChanged || isTypeChanged) {
                    detail.setCriteriaName(dto.getCriteriaName());
                    detail.setWeight(dto.getWeight());
                    detail.setDescription(dto.getDescription());
                    detail.setCriteriaType(dto.getType());
                    // Chỉ cập nhật type nếu trên request thực sự có truyền dữ liệu mới lên
                    detail.setCriteriaType(dto.getType());
                    updateList.add(detail); // Thêm vào danh sách để cập nhật dữ liệu xuống DB
                } else {
                    updateList.add(detail); // Giữ nguyên, không đổi dữ liệu thì Hibernate tự động bỏ qua lệnh Update
                }

            } else {
                detail = new CriteriaDetail();
                detail.setCriteriaSet(criteriaSet);
                detail.setCriteriaName(dto.getCriteriaName());
                detail.setWeight(dto.getWeight());
                detail.setDescription(dto.getDescription());
                detail.setCriteriaType(dto.getType());
                detail.setCriteriaSet(criteriaSet);
                updateList.add(detail);
            }
        }
        auditService.saveLog(
                userDetails.getAccount(),
                AuditAction.UPDATE_CRITERIA,
                AuditEntityType.CRITERIA,
                criteriaSet.getCriteriaSetId(),
                "Update criteria " + criteriaSet.getCriteriaSetName()
        );
        //Save
        List<CriteriaDetail> finalSavedDetails = criteriaDetailRepository.saveAll(updateList);
        return mapToResponse(savedCriteriaSet, finalSavedDetails);

    }

    // 7. Xoa bo tieu chi
    @Override
    // Xóa bộ tiêu chí khi bộ đó không còn bị ràng buộc bởi dữ liệu chấm thi đang sử dụng.
    public void deleteCriteriaSet(Integer criteriaSetId, CustomUserDetails userDetails) {
        // Check Coordinator mới là người được tạo
        Account eventCoordinator = userDetails.getAccount();
        EventCoordinator coordinator =
                eventCoordinatorRepository
                        .findByAccount_AccountId(eventCoordinator.getAccountId())
                        .orElseThrow(() -> new BadRequestException(
                                "Bạn không có quyền truy cập vào bộ tiêu chí để thực hiện thao tác xóa dữ liệu bộ tiêu chí"
                        ));
        CriteriaSet criteriaSet = criteriaSetRepository.findByCriteriaSetId(criteriaSetId);
        if (criteriaSet == null) {
            throw new BadRequestException("Không tìm thấy bộ tiêu chí với Id: " + criteriaSetId);
        }

        criteriaSetRepository.delete(criteriaSet);
        auditService.saveLog(
                userDetails.getAccount(),
                AuditAction.DELETE_CRITERIA,
                AuditEntityType.CRITERIA,
                criteriaSet.getCriteriaSetId(),
                "Xóa bộ tiêu chí " + criteriaSet.getCriteriaSetName()
        );
    }

    @Override
    // Kiểm tra quyền và lấy lịch sử tạo, cập nhật hoặc xóa của bộ tiêu chí.
    public CriteriaHistoryResponse getHistoryCriteria(CustomUserDetails userDetails, Integer criteriaSetId) {

        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(userDetails.getAccount().getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức. Bạn không có quyền truy cập."));

        CriteriaSet criteriaSet = criteriaSetRepository.findByCriteriaSetId(criteriaSetId);

        CriteriaHistoryResponse historyResponse = new CriteriaHistoryResponse();

        historyResponse.setAccountId(userDetails.getAccount().getAccountId());
        historyResponse.setAccountName(eventCoordinator.getCoordinatorName());

        historyResponse.setCriteriaSetId(criteriaSet.getCriteriaSetId());
        historyResponse.setCriteriaSetName(criteriaSet.getCriteriaSetName());

        List<CriteriaHistoryResponse.EventInfo> responses = criteriaSet.getRounds()
                .stream()
                .filter(round1 -> round1.getHackathonEvent() != null)
                .map(round1 -> {
                    HackathonEvent event = round1.getHackathonEvent();
                    CriteriaHistoryResponse.EventInfo info = new CriteriaHistoryResponse.EventInfo();
                    info.setEventId(event.getEventId());
                    info.setEventName(event.getEventName());

                    return info;
                })
                .distinct()
                .toList();

        historyResponse.setEventInfo(responses);

        return historyResponse;
    }

    private CriteriaSetResponseDTO mapToResponse(CriteriaSet criteriaSet, List<CriteriaDetail> details) {
        CriteriaSetResponseDTO response = new CriteriaSetResponseDTO();
        response.setCriteriaSetId(criteriaSet.getCriteriaSetId());
        response.setCriteriaSetName(criteriaSet.getCriteriaSetName());
        response.setMaxScore(criteriaSet.getMaxScore());

        List<CriteriaDetailResponseDTO> listDetails = new ArrayList<>();
        for (CriteriaDetail detail : details) {
            CriteriaDetailResponseDTO dto = new CriteriaDetailResponseDTO();
            dto.setCriteriaId(detail.getCriteriaId());
            dto.setCriteriaName(detail.getCriteriaName());
            dto.setWeight(detail.getWeight());
            dto.setType(detail.getCriteriaType());
            dto.setDescription(detail.getDescription());
            listDetails.add(dto);
        }
        response.setCriteriaDetails(listDetails);

        return response;
    }


}
