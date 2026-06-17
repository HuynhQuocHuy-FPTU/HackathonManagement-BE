package com.hackathon.repository;

import com.hackathon.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Integer> {
     boolean existsByTeamNameIgnoreCase(String teamName);
@Query("SELECT COUNT(t) > 0 FROM Team t " +
        "JOIN t.registrations r " +
        "WHERE LOWER(t.teamName) = LOWER(:teamName) AND r.hackathonEvent.eventId= :eventId")
boolean existsTeamNameInEvent(@Param("teamName") String teamName, @Param("eventId") Integer eventId);

}
