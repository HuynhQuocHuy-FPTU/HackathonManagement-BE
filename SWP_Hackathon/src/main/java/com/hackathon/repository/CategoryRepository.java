package com.hackathon.repository;

import com.hackathon.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    @Modifying
    @Transactional
    @Query("DELETE FROM Category c WHERE c.hackathonEvent.eventId = :eventId")
    public void deleteByEventId(@Param("eventId") Integer eventId);

    List<Category> findAllByHackathonEvent_EventId(int hackathonEventEventId);

    Optional<Category> findCategoryByCategoryIdAndHackathonEvent_EventId(Integer categoryId, int hackathonEventEventId);
}
