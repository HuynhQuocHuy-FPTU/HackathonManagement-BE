package com.hackathon.service;

import com.hackathon.dto.criteria.*;
import com.hackathon.entity.Account;
import com.hackathon.entity.CriteriaDetail;
import com.hackathon.entity.CriteriaSet;
import com.hackathon.entity.EventCoordinator;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.CriteriaDetailRepository;
import com.hackathon.repository.CriteriaSetRepository;
import com.hackathon.repository.EventCoordinatorRepository;
import com.hackathon.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class CriteriaSetServiceImpl implements CriteriaSetService {

    private final CriteriaSetRepository criteriaSetRepository;
    private final CriteriaDetailRepository criteriaDetailRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;

    // 1. Get all bo tieu chi hien co(criteria-set)
    @Override
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
                            d.getDescription()
                    ))
                    .toList();

            dto.setCriteriaDetails(details);

            return dto;
        }).toList();
    }

    //2. Lay tat ca thong tin trong tieu chi chi tiet(detail) hien thi
    @Override
    public List<CriteriaDetailResponseDTO> getAllCriteriaDetail() {

        return criteriaDetailRepository.findAll()
                .stream()
                .map(cri -> new CriteriaDetailResponseDTO(
                        cri.getCriteriaId(),
                        cri.getCriteriaName(),
                        cri.getWeight(),
                        cri.getDescription()
                ))
                .toList();
    }

    //4.Thong qua ID Cua criteriaSet lay duoc ds criteriaDetail tuong ung vs id cua Set
    @Override
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
        // 3. Map DTO
        return mapToResponse(criteriaSet, details);

    }

    //5. Tao CriteriaSet
    @Override
    public CriteriaSetResponseDTO createCriteriaSet(CriteriaSetRequestDTO request, CustomUserDetails userDetails) {
        // Check Coordinator mới là người được tạo

//        Account eventCoordinator = userDetails.getAccount();
//        EventCoordinator coordinator = eventCoordinatorRepository.findById(eventCoordinator.getAccountId())
//                .orElseThrow(() -> new BadRequestException("Bạn không có quyền truy cập vào bộ tiêu chí để thực hiện thao tác tạo bộ tiêu chí."));

//        if (userDetails == null) {
//            throw new RuntimeException("User chưa đăng nhập");
//        }
//

        Account eventCoordinator = userDetails.getAccount();
        EventCoordinator coordinator = eventCoordinatorRepository.findByAccount_AccountId(eventCoordinator.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không có quyền truy cập vào bộ tiêu chí..."));


//        // Thử tìm trong DB bằng account_id
//        var testCoordinator = eventCoordinatorRepository.findByAccount_AccountId(eventCoordinator.getAccountId());
//        System.out.println("5. Tìm Coordinator theo Account ID trong DB có thấy không?: " + testCoordinator.isPresent());
//        System.out.println("------------------------");
        // 1. Tao CriteriaSet
        CriteriaSet criteriaSet = new CriteriaSet();
        criteriaSet.setCriteriaSetName(request.getCriteriaSetName());
        criteriaSet.setMaxScore(request.getMaxScore());
//        criteriaSet.setEventCoordinator(account.getEventCoordinator());
        criteriaSet.setEventCoordinator(coordinator);

        // 2.Tao 1 list de luu Criteria-detail
        List<CriteriaDetail> list = new ArrayList<>();
        for (CriteriaDetailRequestDTO dto : request.getCriteriaDetails()) {
            CriteriaDetail detail = new CriteriaDetail();
            detail.setCriteriaName(dto.getCriteriaName());
            detail.setWeight(dto.getWeight());
            detail.setDescription(dto.getDescription());
            detail.setCriteriaSet(criteriaSet);
            list.add(detail);
        }
        criteriaSet.setCriteriaDetails(list);
        // 3. Luu du lieu xuong DB
        CriteriaSet saved = criteriaSetRepository.save(criteriaSet);

        // Load lại list thực tế vừa lưu thành công , để cập nhật lại ID
//        List<CriteriaDetail> savedDetails = criteriaDetailRepository.findByCriteriaSet_CriteriaSetId(saved.getCriteriaSetId());
        List<CriteriaDetail> savedDetails = saved.getCriteriaDetails();        // 4. Tra du lieu ve DTO
        return mapToResponse(saved, savedDetails);

    }

    // 6. Update CriteriaSet(Có thể thêm xóa , sữa các tiêu chí chi tiết , nhưng không được xóa tiêu chí cha)
    @Override
    @Transactional
    public CriteriaSetResponseDTO updateCriteriaSet(CriteriaSetRequestDTO request, CustomUserDetails userDetails) {
        // Check Coordinator mới là người được tạo
        Account eventCoordinator = userDetails.getAccount();
        EventCoordinator coordinator =
                eventCoordinatorRepository
                        .findByAccount_AccountId(eventCoordinator.getAccountId())
                        .orElseThrow(() -> new BadRequestException(
                                "Bạn không có quyền truy cập vào bộ tiêu chí để thực hiện thao tác cập nhật dữ liệu bộ tiêu chí"
                        ));
        //1. Lay bo tieu chi can update
        CriteriaSet criteriaSet = criteriaSetRepository
                .findByCriteriaSetId(request.getCriteriaSetId());
        if (criteriaSet == null) {
            throw new RuntimeException("CriteriaSet not found with id: " + request.getCriteriaSetId());
        }

        //2.Update info of criteria set
        criteriaSet.setCriteriaSetName(request.getCriteriaSetName());
        criteriaSet.setMaxScore(request.getMaxScore());
        CriteriaSet saved = criteriaSetRepository.save(criteriaSet);

        //2.1 Lay ds criteria-detail thong qua ID cua Set(DB)
        List<CriteriaDetail> listDetail = criteriaDetailRepository.findByCriteriaSet_CriteriaSetId(request.getCriteriaSetId());

        // 3. Delete các tiêu chí detail
        // 3.2 Dùng Set để lưu các tiêu chí Detail thong qua id
        // Lấy ID từ request để check tiêu chí nào đã bị xóa
        Set<Integer> set = new HashSet<>();
        for (CriteriaDetailRequestDTO dto : request.getCriteriaDetails()) {
            if (dto.getCriteriaId() != null) {
                set.add(dto.getCriteriaId());
            }
        }
        //
        List<CriteriaDetail> deleteList = new ArrayList<>();
        for (CriteriaDetail criDetail : listDetail) {
            if (!set.contains(criDetail.getCriteriaId())) {
                deleteList.add(criDetail);
            }
        }
        criteriaDetailRepository.deleteAll(deleteList);

        //2.2 Update info Of Criteria-detail(cũ or mới thêm )

        List<CriteriaDetail> updateList = new ArrayList<>();
        for (CriteriaDetailRequestDTO dto : request.getCriteriaDetails()) {
            CriteriaDetail detail;
            // Check tiêu chí đó có hay chưa để thêm mới or update
            if (dto.getCriteriaId() != null) {
                detail = criteriaDetailRepository.findById(dto.getCriteriaId())
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy tiêu chí nào!!!"));

            } else {
                detail = new CriteriaDetail();
                detail.setCriteriaSet(criteriaSet);

            }
            detail.setCriteriaName(dto.getCriteriaName());
            detail.setWeight(dto.getWeight());
            detail.setDescription(dto.getDescription());
            detail.setCriteriaSet(criteriaSet);
            updateList.add(detail);
        }

        //Save
        List<CriteriaDetail> finalSavedDetails = criteriaDetailRepository.saveAll(updateList);
        return mapToResponse(saved, finalSavedDetails);
    }

    // 7. Xoa bo tieu chi
    @Override
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
            throw new RuntimeException("CriteriaSet not found with id: " + criteriaSetId);
        }
        criteriaSetRepository.delete(criteriaSet);

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
            dto.setDescription(detail.getDescription());
            listDetails.add(dto);
        }
        response.setCriteriaDetails(listDetails);

        return response;
    }


}
