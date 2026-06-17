package com.hackathon.repository;

import com.hackathon.entity.ExpertAssign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Repository
public interface ExpertAssignRepository extends JpaRepository<ExpertAssign, Integer> {
    public List<ExpertAssign> findByCategoryRound_Round_RoundId(int roundId);
    @Modifying
    @Transactional
    @Query("DELETE FROM ExpertAssign ea WHERE ea.categoryRound.round.hackathonEvent.eventId = :eventId")
    void deleteByEventId(@Param("eventId") Integer eventId) ;
}
