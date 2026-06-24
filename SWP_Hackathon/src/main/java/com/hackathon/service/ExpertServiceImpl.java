package com.hackathon.service;

import com.hackathon.dto.expert.ExpertInfoResponse;
import com.hackathon.dto.team.TeamDetailResponse;
import com.hackathon.entity.Expert;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.ExpertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpertServiceImpl implements ExpertService{

    @Autowired
    private ExpertRepository expertRepository;

    @Override
    public List<ExpertInfoResponse> getAllExperts() {
        List<Expert> experts = expertRepository.findAll();

        //chuyển expert thành response
        return experts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public ExpertInfoResponse getExpertById(Integer id) {
        Expert expert = expertRepository.findById(id).orElseThrow(() -> new BadRequestException("Không tìm thấy expert"));

        return this.mapToResponse(expert);
    }

    @Override
    public ExpertInfoResponse mapToResponse(Expert expert) {
        if(expert == null) return null;
        return ExpertInfoResponse.builder()
                .expertId(expert.getExpertId())
                .expertName(expert.getExpertName())
                .type(expert.getType())
                .workplace(expert.getWorkplace()).build();
    }


}
