package com.hackathon.repository;

import com.hackathon.entity.CategoryRound;
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
}