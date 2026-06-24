package com.hackathon.repository;

import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.ExpertAssign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertAssignRepository extends JpaRepository<ExpertAssign, Integer> {
    public List<ExpertAssign> findByCategoryRound_Round_RoundId(int roundId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ExpertAssign ea WHERE ea.categoryRound.round.hackathonEvent.eventId = :eventId")
    void deleteByEventId(@Param("eventId") Integer eventId);

    @Query("SELECT ex FROM ExpertAssign ex " +
            "JOIN ex.categoryRound cr " +
            "JOIN cr.round r " +
            "WHERE ex.expert.expertId = :expertId " +
            "AND r.hackathonEvent.eventId = :eventId")
    List<ExpertAssign> findExpertAssignments(@Param("expertId") Integer expertId, @Param("eventId") Integer eventId);


    //  tìm ExpertAssign phụ trách đúng Team tại CategoryRound cụ thể
    @Query("SELECT ex FROM ExpertAssign ex " +
            "JOIN ex.categoryRound cr " +
            "JOIN Participant p ON p.categoryRound = cr " +
            "JOIN p.registration r " +
            "WHERE r.team.teamId = :teamId " +
            "AND cr.categoryRoundId = :categoryRoundId")
    Optional<ExpertAssign> findExpertAssignByTeamAndCategoryRound(
            @Param("teamId") Integer teamId,
            @Param("categoryRoundId") Integer categoryRoundId);

}
