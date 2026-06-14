package com.hackathon.repository;

import com.hackathon.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Integer> {
//     boolean existsByTeamNameIgnoreCase(String teamName);
//
//     boolean existsByTeamNameIgnoreCaseAndTeamIdNot(String teamName, int teamId);
}
