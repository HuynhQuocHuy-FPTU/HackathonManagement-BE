package com.hackathon.repository;

import com.hackathon.entity.Participant;
import com.hackathon.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {
    boolean existsByTeamNameIgnoreCase(String teamName);

    boolean existsByTeamNameIgnoreCaseAndTeamIdNot(String name, Integer teamId);

    // Tìm những team mà expert được phân công quản lý
    @Query ("SELECT DISTINCT t FROM Team  t " +
            "JOIN Registration  r ON r.team = t " +
            "JOIN HackathonEvent e ON r.hackathonEvent = e " +
            "JOIN Category c ON c.hackathonEvent = e " +
            "JOIN CategoryRound  cr ON cr.category = c " +
            "JOIN ExpertAssign ex ON ex.categoryRound =cr " +
            "WHERE ex.expert.expertId = :expertId")
    List<Team> findTeamsByExpertAssignment(@Param("expertId") Integer expertId);

    // Truy vấn lấy Participant dựa trên ExpertID phân công qua CategoryRound
    @Query("SELECT p FROM Participant p " +
            "JOIN p.registration r " +
            "JOIN r.team t " +
            "JOIN p.categoryRound cr " +
            "JOIN ExpertAssign ea ON ea.categoryRound = cr " +
            "WHERE ea.expert.expertId = :expertId")
    List<Participant> findParticipantsByExpertAssignment(@Param("expertId") Integer expertId);

    @Query("SELECT DISTINCT t FROM Team t " +
            "JOIN Registration r ON r.team = t " +
            "JOIN Participant p ON r.participant = p " +
            "JOIN CategoryRound cr ON p.categoryRound = cr " +
            "JOIN ExpertAssign ea ON ea.categoryRound = cr " +
            "WHERE t.teamId = :teamId AND ea.expert.expertId = :expertId")
    Optional<Team> findTeamByIdAndExpertAssignment(@Param("teamId") Integer teamId, @Param("expertId") Integer expertId);
}

