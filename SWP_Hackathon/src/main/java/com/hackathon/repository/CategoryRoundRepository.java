package com.hackathon.repository;

import com.hackathon.entity.CategoryRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRoundRepository extends JpaRepository<CategoryRound, Integer> {
    public void deleteCategoryRoundsByRound_HackathonEvent_EventId(int eventId);
}