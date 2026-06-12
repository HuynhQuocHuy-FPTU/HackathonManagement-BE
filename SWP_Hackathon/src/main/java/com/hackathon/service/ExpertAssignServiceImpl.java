package com.hackathon.service;

import com.hackathon.dto.category.CategoryExpertAssignRequestDTO;
import com.hackathon.dto.category.CategoryExpertAssignResponseDTO;
import com.hackathon.dto.expert.ExpertAssignmentResponseDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.ExpertAssignRepository;
import com.hackathon.repository.ExpertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExpertAssignServiceImpl implements ExpertAssignService{

    @Autowired
    private ExpertRepository expertRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ExpertAssignRepository expertAssignRepository;

    @Override
    public void assignExpertsToCategoryRound(List<CategoryRound> saveCateRound, List<CategoryExpertAssignRequestDTO> requests) {
        if(requests == null || requests.isEmpty()){
            return;
        }
        List<ExpertAssign> allAssignements = new ArrayList<>();

        for(CategoryExpertAssignRequestDTO cateExpertAssign : requests){
            // kiểm tra xem category này đã được áp dụng chưa
            CategoryRound cateRound = saveCateRound.stream().filter(cr -> cr.getCategory().getCategoryId() == cateExpertAssign.getCategoryId())
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Category này chưa áp dụng vào round"));
            if(cateExpertAssign.getExperts() != null && !cateExpertAssign.getExperts().isEmpty()){
                for(var expertRequest : cateExpertAssign.getExperts()){
                    Expert expert = expertRepository.findById(expertRequest.getExpertId()).orElseThrow(() -> new BadRequestException("Không tìm thấy expert với id: " + expertRequest.getExpertId()));

                    Account expertAccount = expert.getAccount();
                    if(expertAccount != null && expertAccount.getStatus().equals(AccountStatus.INACTIVE)){
                        expertAccount.setStatus(AccountStatus.ACTIVE);
                        accountRepository.save(expertAccount);
                    }
                    ExpertAssign assign = ExpertAssign.builder()
                            .categoryRound(cateRound)
                            .expert(expert)
                            .role(expertRequest.getRole())
                            .build();
                    allAssignements.add(assign);
                }
            }
        }
        if(!allAssignements.isEmpty()){
            expertAssignRepository.saveAll(allAssignements);
        }
    }

    @Override
    public List<CategoryExpertAssignResponseDTO> getExpertAssignmentsByRound(Round round) {
        //1. Khởi tạo list để lưu kết quả
        List<CategoryExpertAssignResponseDTO> expertResponses = new ArrayList<>();
        // nếu round null trả về list rỗng
        if(round == null){
            return expertResponses;
        }
        //2. Lấy các expert assign thuộc về round này
        List<ExpertAssign> assigns = expertAssignRepository.findByCategoryRound_Round_RoundId(round.getRoundId());

        if(assigns == null || assigns.isEmpty()){
            return expertResponses;
        }

        //3. Biến List thành map lưu theo từng category
        Map<Category, List<ExpertAssign>> groupByCategory = assigns.stream().collect(Collectors.groupingBy(assign -> assign.getCategoryRound().getCategory()));

        //4. Duyệt từng nhóm trong Map
        for(Map.Entry<Category, List<ExpertAssign>> entry : groupByCategory.entrySet()){
            Category category = entry.getKey();
            //danh sách chứa các expert DTO thuộc riêng về category
            List<ExpertAssignmentResponseDTO> expertDTOs = new ArrayList<>();

            for(ExpertAssign assign : assigns){
                Expert expert = assign.getExpert();

                //chuyển expert entity sang response
                ExpertAssignmentResponseDTO expertDTO = ExpertAssignmentResponseDTO.builder()
                        .expertId(expert.getExpertId())
                        .expertName(expert.getExpertName())
                        .role(assign.getRole())
                        .build();

                //lưu expert này vào danh dách của Category
                expertDTOs.add(expertDTO);
            }

            CategoryExpertAssignResponseDTO cateExpertAssign = CategoryExpertAssignResponseDTO.builder()
                    .categoryId(category.getCategoryId())
                    .experts(expertDTOs).build();

            expertResponses.add(cateExpertAssign);
        }

        return expertResponses;
    }
}
