package com.hackathon.repository;

import com.hackathon.entity.ExpertAssign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpertAssignRepository extends JpaRepository<ExpertAssign, Integer> {
    public List<ExpertAssign> findByCategoryRound_Round_RoundId(int roundId);
    void deleteExpertAssignByCategoryRound_Round_HackathonEvent_EventId(Integer eventId);
}
