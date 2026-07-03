package com.hackathon.repository;

import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.ExpertAssign;
import com.hackathon.entity.enums.ExpertRole;
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

    @Query("SELECT ex FROM ExpertAssign ex " +
            "JOIN ex.categoryRound cr " +
            "JOIN cr.round r " +
            "WHERE ex.expert.expertId = :expertId " +
            "AND ex.role = :role " +
            "AND r.hackathonEvent.eventId = :eventId")
    List<ExpertAssign> findExpertAssignmentsByRole(@Param("expertId") Integer expertId,
                                                   @Param("role") ExpertRole role,
                                                   @Param("eventId") Integer eventId);

    @Query("SELECT ex FROM ExpertAssign ex " +
            "WHERE ex.categoryRound.categoryRoundId = :categoryRoundId")
    List<ExpertAssign> findByCategoryRoundId(@Param("categoryRoundId") Integer categoryRoundId);

    //    //  tìm ExpertAssign phụ trách đúng Team tại CategoryRound cụ thể
//    @Query("SELECT ex FROM ExpertAssign ex " +
//            "JOIN ex.categoryRound cr " +
//            "JOIN TeamParticipant p ON p.categoryRound = cr " +
//            "JOIN p.registration r " +
//            "WHERE r.team.teamId = :teamId " +
//            "AND cr.categoryRoundId = :categoryRoundId " +
//            "AND ex.expert.expertId = :expertId " +
//            "AND ex.role = 'MENTOR'")
//    Optional<ExpertAssign> findMentorByExpertIdAndCategoryRoundId(
//            @Param("teamId") Integer teamId,
//            @Param("categoryRoundId") Integer categoryRoundId,
//            @Param("expertId" ) Integer expertId);

    @Query("SELECT ex FROM ExpertAssign ex " +
            "WHERE ex.categoryRound.categoryRoundId = :categoryRoundId " +
            "AND ex.expert.expertId = :expertId " +
            "AND ex.role = 'MENTOR'")
    Optional<ExpertAssign> findMentorByExpertIdAndCategoryRoundId(
            @Param("categoryRoundId") Integer categoryRoundId,
            @Param("expertId") Integer expertId);

    List<ExpertAssign> findByCategoryRound_CategoryRoundIdAndRole(
            Integer categoryRoundId,
            ExpertRole role);
}
