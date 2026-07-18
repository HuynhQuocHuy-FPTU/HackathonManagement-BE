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
public interface CategoryRoundRepository extends JpaRepository<CategoryRound, Integer> {
    @Modifying
    @Transactional
    @Query("DELETE FROM CategoryRound  cr WHERE cr.round.hackathonEvent.eventId = :eventId")
    public void deleteByEventId(@Param("eventId") Integer eventId);

    @Transactional
    @Query("SELECT cr FROM CategoryRound cr WHERE cr.category.categoryId = :cateId AND cr.round.roundId = :roundId")
    Optional<CategoryRound> findCategoryRoundByCategoryAndRound(@Param("cateId") Integer cateId, @Param("roundId") Integer roundId);

    Optional<CategoryRound> findCategoryRoundByCategory_CategoryIdAndRound_RoundId(Integer categoryCategoryId, Integer roundRoundId);

    @Query("SELECT cr FROM TeamParticipant p " +
            "JOIN p.categoryRound cr " +
            "JOIN p.registration rg " +
            "JOIN rg.team t " +
            "WHERE t.teamId = :teamId " +
            "AND rg.status = RegistrationStatus.APPROVED " +
            "AND p.status = ParticipantStatus.ACTIVE " +
            "AND cr.round.status = RoundStatus.ONGOING")
    List<CategoryRound> findActiveCategoryRoundsByTeamId(@Param("teamId") Integer teamId);


    List<CategoryRound> findByRound_HackathonEvent_EventId(Integer eventId);

    List<CategoryRound> findCategoryRoundByRound_RoundId(Integer roundRoundId);


}