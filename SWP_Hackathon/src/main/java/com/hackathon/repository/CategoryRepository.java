package com.hackathon.repository;

import com.hackathon.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    public void deleteCategoriesByHackathonEvent_EventId(int eventId);
}
