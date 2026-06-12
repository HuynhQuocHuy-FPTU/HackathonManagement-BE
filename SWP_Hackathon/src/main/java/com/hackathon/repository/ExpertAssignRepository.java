package com.hackathon.repository;

import com.hackathon.entity.ExpertAssign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ExpertAssignRepository extends JpaRepository<ExpertAssign, Integer> {
    public List<ExpertAssign> findByCategoryRound_Round_RoundId(int roundId);
    void deleteExpertAssignByCategoryRound_Round_HackathonEvent_EventId(Integer eventId);
}
