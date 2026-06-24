package com.hackathon.repository;

import com.hackathon.entity.TeamRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRequestRepository extends JpaRepository<TeamRequest,Integer> {
    @Query("SELECT tr FROM TeamRequest tr " +
            "JOIN  tr.expertAssign ex " +
            "WHERE tr.expertAssign.expert.expertId = ex.expert.expertId ")
    List<TeamRequest> findRequestByExpertId(Integer expertID);
}
