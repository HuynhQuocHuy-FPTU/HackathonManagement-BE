package com.hackathon.repository;

import com.hackathon.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Integer> {
     boolean existsByTeamName(String teamName);
}
